package lv.janis.iom.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import lv.janis.iom.dto.filters.ListProductFilter;
import lv.janis.iom.dto.requests.ProductCreationRequest;
import lv.janis.iom.dto.requests.ProductUpdateRequest;
import lv.janis.iom.dto.response.ProductResponse;
import lv.janis.iom.service.ProductService;


@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreationRequest request) {
        var product = productService.createProduct(request);
        var response = ProductResponse.from(product);
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(product.getId())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> listProducts(
        @Valid @ModelAttribute ListProductFilter filter,
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        var page = productService.listProducts(filter, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/deleted")
    public ResponseEntity<Page<ProductResponse>> listDeletedProducts(
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable
    ) {
        var page = productService.listDeletedProducts(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        var product = productService.getProductById(id);
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
        @PathVariable Long id,
        @Valid @RequestBody ProductUpdateRequest request
    ) {
        var product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ProductResponse> deactivateProduct(@PathVariable Long id) {
        var product = productService.deactivateProduct(id);
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ProductResponse> activateProduct(@PathVariable Long id) {
        var product = productService.activateProduct(id);
        return ResponseEntity.ok(ProductResponse.from(product));
    }
    
}
