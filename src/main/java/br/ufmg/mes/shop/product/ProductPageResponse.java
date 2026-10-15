package br.ufmg.mes.shop.product;

import java.util.List;

import org.springframework.data.domain.Page;

public class ProductPageResponse {

    private final List<ProductResponse> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public ProductPageResponse(Page<ProductResponse> products) {
        this.content = products.getContent();
        this.page = products.getNumber();
        this.size = products.getSize();
        this.totalElements = products.getTotalElements();
        this.totalPages = products.getTotalPages();
    }

    public List<ProductResponse> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
