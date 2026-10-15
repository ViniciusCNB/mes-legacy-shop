package br.ufmg.mes.shop.product;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService products;

    public ProductController(ProductService products) {
        this.products = products;
    }

    @GetMapping
    public ProductPageResponse list(
            @RequestParam(name = "page", defaultValue = "0")
            @Min(value = 0, message = "deve estar entre 0 e 1000000")
            @Max(value = 1000000, message = "deve estar entre 0 e 1000000") int page,
            @RequestParam(name = "size", defaultValue = "20")
            @Min(value = 1, message = "deve estar entre 1 e 100")
            @Max(value = 100, message = "deve estar entre 1 e 100") int size) {
        return products.list(page, size);
    }

    @GetMapping("/{id}")
    public ProductResponse find(
            @PathVariable("id") @Positive(message = "deve ser positivo") Long id) {
        return products.find(id);
    }
}
