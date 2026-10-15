package br.ufmg.mes.shop.product;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    public ProductPageResponse list(int page, int size) {
        return new ProductPageResponse(products.findByActiveTrue(
                PageRequest.of(page, size, Sort.by("id").ascending())).map(ProductResponse::new));
    }

    public ProductResponse find(long id) {
        return products.findByIdAndActiveTrue(id)
                .map(ProductResponse::new)
                .orElseThrow(ProductNotFoundException::new);
    }
}
