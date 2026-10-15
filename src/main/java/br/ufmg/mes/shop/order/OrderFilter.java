package br.ufmg.mes.shop.order;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.springframework.data.jpa.domain.Specification;

public class OrderFilter {
    private final Long customerId;
    private final OrderStatus status;
    private final Instant start;
    private final Instant end;

    public OrderFilter(Long customerId, OrderStatus status, String start, String end) {
        this.customerId = customerId;
        this.status = status;
        this.start = parse("start", start);
        this.end = parse("end", end);
        if (this.start != null && this.end != null && !this.start.isBefore(this.end)) {
            throw new InvalidOrderRequestException("end", "deve ser posterior a start");
        }
    }

    private static Instant parse(String field, String value) {
        if (value == null) { return null; }
        try {
            if (!value.endsWith("Z")) { throw new DateTimeParseException("UTC required", value, 0); }
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw new InvalidOrderRequestException(field, "deve ser um instante ISO-8601 com Z");
        }
    }

    public Specification<PurchaseOrder> specification() {
        Specification<PurchaseOrder> filter = (root, query, builder) -> builder.conjunction();
        if (customerId != null) {
            filter = filter.and((root, query, builder) -> builder.equal(root.get("customer").get("id"), customerId));
        }
        if (status != null) {
            filter = filter.and((root, query, builder) -> builder.equal(root.get("status"), status));
        }
        if (start != null) {
            filter = filter.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), start));
        }
        if (end != null) {
            filter = filter.and((root, query, builder) -> builder.lessThan(root.get("createdAt"), end));
        }
        return filter;
    }
}
