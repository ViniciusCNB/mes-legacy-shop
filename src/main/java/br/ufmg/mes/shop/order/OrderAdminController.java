package br.ufmg.mes.shop.order;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin")
public class OrderAdminController {
    private final OrderService orders;

    public OrderAdminController(OrderService orders) { this.orders = orders; }

    @GetMapping("/orders")
    public OrderPageResponse list(@RequestParam(required = false) @Positive Long customerId,
                                  @RequestParam(required = false) OrderStatus status,
                                  @RequestParam(required = false) String start,
                                  @RequestParam(required = false) String end,
                                  @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return orders.list(new OrderFilter(customerId, status, start, end), page, size);
    }

    @GetMapping("/reports/orders")
    public OrderSummaryResponse summary(@RequestParam(required = false) @Positive Long customerId,
                                        @RequestParam(required = false) String start,
                                        @RequestParam(required = false) String end) {
        return orders.summary(new OrderFilter(customerId, OrderStatus.CREATED, start, end));
    }
}
