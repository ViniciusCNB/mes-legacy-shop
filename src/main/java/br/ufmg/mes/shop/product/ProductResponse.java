package br.ufmg.mes.shop.product;

import java.math.BigDecimal;

public class ProductResponse {

    private final Long id;
    private final String sku;
    private final String name;
    private final BigDecimal price;
    private final int stockQuantity;

    public ProductResponse(Product product) {
        this.id = product.getId();
        this.sku = product.getSku();
        this.name = product.getName();
        this.price = product.getPrice();
        this.stockQuantity = product.getStockQuantity();
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }
}
