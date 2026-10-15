package br.ufmg.mes.shop.api;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolationException;

import br.ufmg.mes.shop.order.InvalidOrderRequestException;
import br.ufmg.mes.shop.order.OrderConflictException;
import br.ufmg.mes.shop.order.OrderNotFoundException;
import br.ufmg.mes.shop.product.ProductNotFoundException;
import br.ufmg.mes.shop.product.SkuAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError orderNotFound(OrderNotFoundException exception) {
        return new ApiError("ORDER_NOT_FOUND", exception.getMessage(), List.of());
    }

    @ExceptionHandler(OrderConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError orderConflict(OrderConflictException exception) {
        return new ApiError(exception.getCode(), exception.getMessage(), List.of());
    }

    @ExceptionHandler(InvalidOrderRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError invalidOrder(InvalidOrderRequestException exception) {
        return new ApiError("INVALID_REQUEST", "Parâmetros inválidos",
                List.of(new ApiError.FieldError(exception.getField(), exception.getMessage())));
    }

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
                List.of(new ApiError.FieldError(exception.getName(), "valor inválido para o parâmetro")));
    }

    @ExceptionHandler(SkuAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError skuConflict(SkuAlreadyExistsException exception) {
        return new ApiError("SKU_ALREADY_EXISTS", exception.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError invalidBody(MethodArgumentNotValidException exception) {
        List<ApiError.FieldError> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldError(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(ApiError.FieldError::getField))
                .collect(Collectors.toList());
        return new ApiError("INVALID_REQUEST", "Corpo inválido", fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError invalidJson() {
        return new ApiError("INVALID_REQUEST", "JSON inválido", List.of());
    }
}
