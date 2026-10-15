package br.ufmg.mes.shop.product;

import java.math.BigDecimal;

public class AdminProductResponse {

    private final Long id;
    private final String sku;
    private final String name;
    private final BigDecimal price;
    private final int stockQuantity;
    private final boolean active;

    public AdminProductResponse(Product product) {
        this.id = product.getId();
        this.sku = product.getSku();
        this.name = product.getName();
        this.price = product.getPrice();
        this.stockQuantity = product.getStockQuantity();
        this.active = product.isActive();
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public int getStockQuantity() { return stockQuantity; }
    public boolean isActive() { return active; }
}
