package br.ufmg.mes.shop.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class OrderResponse {
    private final Long id;
    private final Long customerId;
    private final OrderStatus status;
    private final Instant createdAt;
    private final Instant cancelledAt;
    private final BigDecimal total;
    private final List<Item> items;

    public OrderResponse(PurchaseOrder order) {
        id = order.getId();
        customerId = order.getCustomer().getId();
        status = order.getStatus();
        createdAt = order.getCreatedAt();
        cancelledAt = order.getCancelledAt();
        total = order.getTotal();
        items = order.getItems().stream().map(Item::new).collect(Collectors.toList());
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public BigDecimal getTotal() { return total; }
    public List<Item> getItems() { return items; }

    public static class Item {
        private final Long productId;
        private final int quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal subtotal;

        public Item(OrderItem item) {
            productId = item.getProduct().getId();
            quantity = item.getQuantity();
            unitPrice = item.getUnitPrice();
            subtotal = item.getSubtotal();
        }

        public Long getProductId() { return productId; }
        public int getQuantity() { return quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getSubtotal() { return subtotal; }
    }
}
