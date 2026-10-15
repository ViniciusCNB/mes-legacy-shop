package br.ufmg.mes.shop.order;

import java.util.List;
import org.springframework.data.domain.Page;

public class OrderPageResponse {
    private final List<OrderResponse> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public OrderPageResponse(Page<OrderResponse> orders) {
        content = orders.getContent();
        page = orders.getNumber();
        size = orders.getSize();
        totalElements = orders.getTotalElements();
        totalPages = orders.getTotalPages();
    }

    public List<OrderResponse> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
}
