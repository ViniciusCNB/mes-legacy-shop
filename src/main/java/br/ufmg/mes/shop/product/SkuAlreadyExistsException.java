package br.ufmg.mes.shop.product;

public class SkuAlreadyExistsException extends RuntimeException {

    public SkuAlreadyExistsException() {
        super("SKU já cadastrado");
    }
}
