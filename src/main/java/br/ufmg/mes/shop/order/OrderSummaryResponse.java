package br.ufmg.mes.shop.order;

import java.math.BigDecimal;

public class OrderSummaryResponse {
    private final long orderCount;
    private final BigDecimal total;

    public OrderSummaryResponse(Long orderCount, BigDecimal total) {
        this.orderCount = orderCount;
        this.total = total.setScale(2);
    }

    public long getOrderCount() { return orderCount; }
    public BigDecimal getTotal() { return total; }
}
