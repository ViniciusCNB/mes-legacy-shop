package br.ufmg.mes.shop.order;

public class OrderConflictException extends RuntimeException {
    private final String code;

    public OrderConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}
