package br.ufmg.mes.shop.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import br.ufmg.mes.shop.product.Product;
import br.ufmg.mes.shop.product.ProductNotFoundException;
import br.ufmg.mes.shop.product.ProductRepository;
import br.ufmg.mes.shop.user.AppUser;
import br.ufmg.mes.shop.user.AppUserRepository;
import br.ufmg.mes.shop.user.UserRole;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orders;
    private final ProductRepository products;
    private final AppUserRepository users;
    private final EntityManager entityManager;

    public OrderService(OrderRepository orders, ProductRepository products, AppUserRepository users,
                        EntityManager entityManager) {
        this.orders = orders;
        this.products = products;
        this.users = users;
        this.entityManager = entityManager;
    }

    @Transactional
    public OrderResponse create(String email, CreateOrderRequest request) {
        Set<Long> productIds = new HashSet<>();
        for (CreateOrderRequest.Item item : request.getItems()) {
            if (!productIds.add(item.getProductId())) {
                throw new InvalidOrderRequestException("items", "não deve repetir produtos");
            }
        }
        List<CreateOrderRequest.Item> sorted = request.getItems().stream()
                .sorted(Comparator.comparing(CreateOrderRequest.Item::getProductId)).collect(Collectors.toList());
        PurchaseOrder order = new PurchaseOrder(user(email), Instant.now().truncatedTo(ChronoUnit.MICROS));
        for (CreateOrderRequest.Item item : sorted) {
            Product product = products.findByIdForUpdate(item.getProductId())
                    .orElseThrow(ProductNotFoundException::new);
            if (!product.isActive()) {
                throw new OrderConflictException("PRODUCT_INACTIVE", "Produto inativo");
            }
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new OrderConflictException("INSUFFICIENT_STOCK", "Estoque insuficiente");
            }
            order.addItem(product, item.getQuantity());
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
        }
        return new OrderResponse(orders.saveAndFlush(order));
    }

    public OrderResponse find(long id, String email) {
        PurchaseOrder order = orders.findById(id).orElseThrow(OrderNotFoundException::new);
        checkOwner(order, user(email));
        return new OrderResponse(order);
    }

    public OrderPageResponse own(String email, int page, int size) {
        return list(new OrderFilter(user(email).getId(), null, null, null), page, size);
    }

    public OrderPageResponse list(OrderFilter filter, int page, int size) {
        return new OrderPageResponse(orders.findAll(filter.specification(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))).map(OrderResponse::new));
    }

    public OrderSummaryResponse summary(OrderFilter filter) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<OrderSummaryResponse> query = builder.createQuery(OrderSummaryResponse.class);
        Root<PurchaseOrder> order = query.from(PurchaseOrder.class);
        query.select(builder.construct(OrderSummaryResponse.class, builder.count(order),
                builder.coalesce(builder.<BigDecimal>sum(order.get("total")), BigDecimal.ZERO)));
        query.where(filter.specification().toPredicate(order, query, builder));
        return entityManager.createQuery(query).getSingleResult();
    }

    @Transactional
    public OrderResponse cancel(long id, String email) {
        PurchaseOrder order = orders.findByIdForUpdate(id).orElseThrow(OrderNotFoundException::new);
        checkOwner(order, user(email));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return new OrderResponse(order);
        }
        List<OrderItem> items = order.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId())).collect(Collectors.toList());
        for (OrderItem item : items) {
            Product product = products.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(ProductNotFoundException::new);
            if ((long) product.getStockQuantity() + item.getQuantity() > Integer.MAX_VALUE) {
                throw new OrderConflictException("STOCK_LIMIT_EXCEEDED", "Reposição excede o limite de estoque");
            }
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
        }
        order.cancel(Instant.now().truncatedTo(ChronoUnit.MICROS));
        orders.flush();
        return new OrderResponse(order);
    }

    private AppUser user(String email) {
        return users.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }

    private void checkOwner(PurchaseOrder order, AppUser user) {
        if (user.getRole() != UserRole.ADMIN && !order.getCustomer().getId().equals(user.getId())) {
            throw new OrderNotFoundException();
        }
    }
}
