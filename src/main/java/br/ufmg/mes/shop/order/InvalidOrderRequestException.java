package br.ufmg.mes.shop.order;

public class InvalidOrderRequestException extends RuntimeException {
    private final String field;

    public InvalidOrderRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}
