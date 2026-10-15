package br.ufmg.mes.shop.product;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException() {
        super("Produto não encontrado");
    }
}
