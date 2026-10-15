package br.ufmg.mes.shop.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
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
