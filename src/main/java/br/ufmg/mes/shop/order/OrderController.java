package br.ufmg.mes.shop.order;

import java.net.URI;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) { this.orders = orders; }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@AuthenticationPrincipal UserDetails user,
                                               @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = orders.create(user.getUsername(), request);
        return ResponseEntity.created(URI.create("/api/orders/" + order.getId())).body(order);
    }

    @GetMapping
    public OrderPageResponse list(@AuthenticationPrincipal UserDetails user,
                                 @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
                                 @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return orders.own(user.getUsername(), page, size);
    }

    @GetMapping("/{id}")
    public OrderResponse find(@PathVariable @Positive long id, @AuthenticationPrincipal UserDetails user) {
        return orders.find(id, user.getUsername());
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable @Positive long id, @AuthenticationPrincipal UserDetails user) {
        return orders.cancel(id, user.getUsername());
    }
}
