package br.ufmg.mes.shop.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import br.ufmg.mes.shop.product.Product;
import br.ufmg.mes.shop.user.AppUser;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private AppUser customer;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "cancelled_at")
    private Instant cancelledAt;
    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal total;
    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
    @OrderBy("product.id ASC")
    private List<OrderItem> items = new ArrayList<>();

    protected PurchaseOrder() { }

    public PurchaseOrder(AppUser customer, Instant createdAt) {
        this.customer = customer;
        this.createdAt = createdAt;
        this.status = OrderStatus.CREATED;
        this.total = new BigDecimal("0.00");
    }

    public void addItem(Product product, int quantity) {
        OrderItem item = new OrderItem(this, product, quantity);
        items.add(item);
        total = total.add(item.getSubtotal());
    }

    public void cancel(Instant cancelledAt) {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
    }

    public Long getId() { return id; }
    public AppUser getCustomer() { return customer; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public BigDecimal getTotal() { return total; }
    public List<OrderItem> getItems() { return items; }
}
