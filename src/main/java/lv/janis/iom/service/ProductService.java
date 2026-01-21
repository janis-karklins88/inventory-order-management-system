package lv.janis.iom.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import lv.janis.iom.dto.response.ProductResponse;

import jakarta.persistence.EntityNotFoundException;
import lv.janis.iom.dto.filters.ListProductFilter;
import lv.janis.iom.dto.requests.ProductCreationRequest;
import lv.janis.iom.dto.requests.ProductUpdateRequest;
import lv.janis.iom.entity.Product;
import lv.janis.iom.repository.ProductRepository;
import lv.janis.iom.repository.specification.ProductSpecifications;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Product createProduct(ProductCreationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (productRepository.existsBySku(request.getSku())) {
            throw new IllegalStateException("Product with SKU " + request.getSku() + " already exists.");
        }
        if (productRepository.existsByName(request.getName())) {
            throw new IllegalStateException("Product with name " + request.getName() + " already exists.");
        }

        Product product = Product.create(
            request.getSku(),
            request.getName(),
            request.getDescription(),
            request.getPrice()
        );
        return productRepository.save(product);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Product not found."));
    }

    @Transactional
    public Product updateProduct(Long id, ProductUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        boolean hasUpdates =
        request.getName() != null ||
        request.getDescription() != null ||
        request.getPrice() != null;

        if (!hasUpdates) {
            throw new IllegalStateException("At least one field must be provided for update");
        }
        
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Product not found."));
        
        if (request.getName() != null) {
            if (!request.getName().equals(product.getName())
                && productRepository.existsByName(request.getName())) {
                throw new IllegalStateException("Product with name " + request.getName() + " already exists.");
            }
            product.rename(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.updatePrice(request.getPrice());
        }

        return productRepository.save(product);
    }

    @Transactional
    public Product deactivateProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Product not found."));
        product.deactivate();
        return productRepository.save(product);
    }

    @Transactional
    public Product activateProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Product not found."));
        product.activate();
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(ListProductFilter filter, Pageable pageable) {
        ListProductFilter safeFilter = filter == null ? new ListProductFilter() : filter;
        Pageable safePageable = capPageSize(pageable, 100);
        Specification<Product> spec = Specification
                .where(ProductSpecifications.search(safeFilter.getQuery()))
                .and(ProductSpecifications.skuEquals(safeFilter.getSku()))
                .and(ProductSpecifications.priceGte(safeFilter.getMinPrice()))
                .and(ProductSpecifications.priceLte(safeFilter.getMaxPrice()))
                .and(ProductSpecifications.notDeleted());
        return productRepository.findAll(spec, safePageable).map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listDeletedProducts(Pageable pageable) {
        Pageable safePageable = capPageSize(pageable, 100);
        Specification<Product> spec = Specification.where(ProductSpecifications.deletedOnly());
        return productRepository.findAll(spec, safePageable).map(ProductResponse::from);
    }

    private Pageable capPageSize(Pageable pageable, int maxSize) {
        if (pageable.getPageSize() > maxSize) {
            return PageRequest.of(pageable.getPageNumber(), maxSize, pageable.getSort());
        }
        return pageable;
    }

}
    
