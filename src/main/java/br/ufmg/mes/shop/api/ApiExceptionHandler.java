package br.ufmg.mes.shop.api;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolationException;

import br.ufmg.mes.shop.product.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError productNotFound(ProductNotFoundException exception) {
        return new ApiError("PRODUCT_NOT_FOUND", exception.getMessage(), List.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError invalidParameters(ConstraintViolationException exception) {
        List<ApiError.FieldError> fields = exception.getConstraintViolations().stream()
                .map(violation -> {
                    String path = violation.getPropertyPath().toString();
                    return new ApiError.FieldError(path.substring(path.lastIndexOf('.') + 1),
                            violation.getMessage());
                })
                .sorted(Comparator.comparing(ApiError.FieldError::getField))
                .collect(Collectors.toList());
        return new ApiError("INVALID_REQUEST", "Parâmetros inválidos", fields);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError invalidType(MethodArgumentTypeMismatchException exception) {
        return new ApiError("INVALID_REQUEST", "Parâmetros inválidos",
                List.of(new ApiError.FieldError(exception.getName(), "deve ser um inteiro válido")));
    }
}
