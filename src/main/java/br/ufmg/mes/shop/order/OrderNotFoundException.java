package br.ufmg.mes.shop.order;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException() { super("Pedido não encontrado"); }
}
