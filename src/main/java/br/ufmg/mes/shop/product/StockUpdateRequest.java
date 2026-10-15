package br.ufmg.mes.shop.product;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class StockUpdateRequest {

    @NotNull
    @Min(0)
    private Integer stockQuantity;

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
}
