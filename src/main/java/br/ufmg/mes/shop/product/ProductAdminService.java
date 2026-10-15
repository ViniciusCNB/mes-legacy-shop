package br.ufmg.mes.shop.product;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductAdminService {

    private final ProductRepository products;

    public ProductAdminService(ProductRepository products) {
        this.products = products;
    }

    public AdminProductResponse create(CreateProductRequest request) {
        Product product = new Product(request.getSku(), request.getName(), request.getPrice().setScale(2),
                request.getStockQuantity(), request.getActive());
        try {
            return new AdminProductResponse(products.saveAndFlush(product));
        } catch (DataIntegrityViolationException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException
                        && "products_sku_unique".equals(((ConstraintViolationException) cause).getConstraintName())) {
                    throw new SkuAlreadyExistsException();
                }
            }
            throw exception;
        }
    }

    public AdminProductResponse update(long id, UpdateProductRequest request) {
        Product product = findForUpdate(id);
        product.updateDetails(request.getName(), request.getPrice().setScale(2), request.getActive());
        products.flush();
        return new AdminProductResponse(product);
    }

    public AdminProductResponse setStock(long id, StockUpdateRequest request) {
        Product product = findForUpdate(id);
        product.setStockQuantity(request.getStockQuantity());
        products.flush();
        return new AdminProductResponse(product);
    }

    private Product findForUpdate(long id) {
        return products.findByIdForUpdate(id).orElseThrow(ProductNotFoundException::new);
    }
}
