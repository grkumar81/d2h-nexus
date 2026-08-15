package org.nexus.d2h.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN u.tenants t WHERE t.id = :tenantId")
    Page<User> findByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT u FROM User u JOIN u.tenants t WHERE u.id = :userId AND t.id = :tenantId")
    Optional<User> findByIdAndTenantId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);
}
