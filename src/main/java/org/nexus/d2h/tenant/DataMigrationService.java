package org.nexus.d2h.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * One-time data migration: copies rows from the shared d2h_platform schema
 * (filtered by tenant_id) into each tenant's dedicated schema (without tenant_id).
 *
 * Idempotent — skips tables that already have data in the target schema.
 * Triggered manually via PlatformTenantController or at startup with a flag.
 */
@Slf4j
@Service
public class DataMigrationService {

    private final DataSource platformDataSource;
    private final TenantRepository tenantRepository;

    public DataMigrationService(@Qualifier("platformDataSource") DataSource platformDataSource,
                                TenantRepository tenantRepository) {
        this.platformDataSource = platformDataSource;
        this.tenantRepository = tenantRepository;
    }

    public void migrateAll() {
        List<Tenant> tenants = tenantRepository.findAll();
        log.info("Starting data migration for {} tenant(s)", tenants.size());
        for (Tenant tenant : tenants) {
            if (tenant.getStatus() == TenantStatus.ACTIVE || tenant.getStatus() == TenantStatus.APPROVED) {
                migrateTenant(tenant);
            }
        }
        log.info("Data migration complete");
    }

    public void migrateTenant(Tenant tenant) {
        String schema = tenant.getSchemaName();
        Long tenantId = tenant.getId();
        log.info("Migrating tenant: code={} schema={}", tenant.getTenantCode(), schema);

        try (Connection conn = platformDataSource.getConnection()) {
            migrateTable(conn, tenantId, schema, "retailers",
                    "retailer_code,retailer_name,mobile,alternate_mobile,email,address,city,state,pin_code,gst_number,pan_number,status,joining_date,created_at,updated_at,created_by,updated_by");
            migrateTable(conn, tenantId, schema, "stb_assets",
                    "serial_number,box_number,model,manufacturer,batch,purchase_date,purchase_cost,status,retailer_id,tagging_date,sale_date,activation_date,return_date,created_at,updated_at,created_by,updated_by,version");
            migrateTable(conn, tenantId, schema, "stb_asset_history",
                    "asset_id,from_status,to_status,retailer_id,changed_by,remarks,changed_at");
            migrateTable(conn, tenantId, schema, "stb_sales",
                    "retailer_id,transaction_date,total_amount,payment_status,reference,remarks,created_at,updated_at,created_by");
            migrateTable(conn, tenantId, schema, "financial_transactions",
                    "retailer_id,transaction_type,transaction_status,transaction_date,amount,payment_method,reference,payment_reference,description,remarks,source,sale_id,reversed_by_id,reversal_of_id,created_at,updated_at,created_by,updated_by");
            migrateTable(conn, tenantId, schema, "recharge_transactions",
                    "retailer_id,asset_id,reference,external_reference,recharge_date,amount,recharge_type,recharge_status,payment_method,payment_reference,service_period,description,remarks,source,reversed_by_id,reversal_of_id,created_at,updated_at,created_by,updated_by");
            migrateTable(conn, tenantId, schema, "outbox_events",
                    "event_type,aggregate_id,payload,status,attempts,created_at,processed_at,next_retry_at,error_message,updated_at");
            migrateTable(conn, tenantId, schema, "notification_config",
                    "event_type,channel,enabled,recipients,created_at,updated_at,created_by");
            migrateTable(conn, tenantId, schema, "notification_delivery",
                    "outbox_event_id,channel,recipient,status,attempts,sent_at,error_message,created_at,updated_at");
            migrateTable(conn, tenantId, schema, "audit_logs",
                    "entity_type,entity_id,action,performed_by,details,ip_address,created_at");
        } catch (Exception e) {
            log.error("Migration failed for tenant={}: {}", tenant.getTenantCode(), e.getMessage(), e);
            throw new RuntimeException("Migration failed for tenant: " + tenant.getTenantCode(), e);
        }

        log.info("Migration complete for tenant: code={}", tenant.getTenantCode());
    }

    private void migrateTable(Connection conn, Long tenantId, String targetSchema,
                               String table, String columns) throws SQLException {
        // Check if target already has data — skip if so (idempotent)
        try (Statement check = conn.createStatement();
             ResultSet rs = check.executeQuery(
                     "SELECT COUNT(*) FROM `" + targetSchema + "`.`" + table + "`")) {
            if (rs.next() && rs.getLong(1) > 0) {
                log.debug("Skipping {} — already has data in {}", table, targetSchema);
                return;
            }
        } catch (SQLException e) {
            // Table may not exist yet — skip
            log.debug("Table {} not found in {} — skipping", table, targetSchema);
            return;
        }

        // Check source has rows for this tenant
        String countSql = hasTenantId(table)
                ? "SELECT COUNT(*) FROM d2h_platform.`" + table + "` WHERE tenant_id = " + tenantId
                : null;

        if (countSql != null) {
            try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(countSql)) {
                if (rs.next() && rs.getLong(1) == 0) {
                    log.debug("No rows to migrate for {} tenant={}", table, tenantId);
                    return;
                }
            }
        }

        String selectSql = hasTenantId(table)
                ? "SELECT " + columns + " FROM d2h_platform.`" + table + "` WHERE tenant_id = " + tenantId
                : "SELECT " + columns + " FROM d2h_platform.`" + table + "`";

        String[] cols = columns.split(",");
        String placeholders = String.join(",", java.util.Collections.nCopies(cols.length, "?"));
        String insertSql = "INSERT INTO `" + targetSchema + "`.`" + table + "` (" + columns + ") VALUES (" + placeholders + ")";

        int count = 0;
        try (Statement sel = conn.createStatement();
             ResultSet rs = sel.executeQuery(selectSql);
             PreparedStatement ins = conn.prepareStatement(insertSql)) {

            while (rs.next()) {
                for (int i = 0; i < cols.length; i++) {
                    ins.setObject(i + 1, rs.getObject(cols[i].trim()));
                }
                ins.addBatch();
                count++;
                if (count % 500 == 0) {
                    ins.executeBatch();
                }
            }
            if (count % 500 != 0) {
                ins.executeBatch();
            }
        }
        log.info("Migrated {} rows into {}.{}", count, targetSchema, table);
    }

    private boolean hasTenantId(String table) {
        return switch (table) {
            case "retailers", "stb_assets", "stb_asset_history", "stb_sales",
                 "financial_transactions", "recharge_transactions", "outbox_events",
                 "notification_config", "notification_delivery" -> true;
            default -> false;
        };
    }
}
