package org.nexus.d2h.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String code;
    private final String message;
    private final Instant timestamp;
    private final String traceId;
    private final List<FieldError> fieldErrors;

    private ErrorResponse(String code, String message, String traceId, List<FieldError> fieldErrors) {
        this.code = code;
        this.message = message;
        this.timestamp = Instant.now();
        this.traceId = traceId;
        this.fieldErrors = fieldErrors;
    }

    public static ErrorResponse of(String code, String message, String traceId) {
        return new ErrorResponse(code, message, traceId, null);
    }

    public static ErrorResponse ofValidation(String message, String traceId, List<FieldError> fieldErrors) {
        return new ErrorResponse("VALIDATION_ERROR", message, traceId, fieldErrors);
    }

    public record FieldError(String field, String message) {}
}
