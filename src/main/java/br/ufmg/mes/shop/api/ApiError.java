package br.ufmg.mes.shop.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ApiError {

    private final String code;
    private final String message;
    private final List<FieldError> fieldErrors;

    public ApiError(String code, String message, List<FieldError> fieldErrors) {
        this.code = code;
        this.message = message;
        this.fieldErrors = fieldErrors;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public static class FieldError {

        private final String field;
        private final String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }
    }
}
