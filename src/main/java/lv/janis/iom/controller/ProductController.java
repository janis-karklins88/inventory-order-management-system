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

import org.springframework.lang.NonNull;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import jakarta.validation.Valid;
import lv.janis.iom.dto.filters.ListProductFilter;
import lv.janis.iom.dto.requests.ProductCreationRequest;
import lv.janis.iom.dto.requests.ProductUpdateRequest;
import lv.janis.iom.dto.response.ProductResponse;
import lv.janis.iom.service.ProductService;

@Tag(name = "Products", description = "Product management endpoints")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "Create product")
    @ApiResponse(responseCode = "201", description = "Product created")
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

    @Operation(summary = "List products")
    @ApiResponse(responseCode = "200", description = "Products listed")
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> listProducts(
            @Parameter(description = "Filter options") @ParameterObject @Valid @ModelAttribute ListProductFilter filter,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) @NonNull Pageable pageable) {
        var page = productService.listProducts(filter, pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "List deleted products")
    @ApiResponse(responseCode = "200", description = "Deleted products listed")
    @GetMapping("/deleted")
    public ResponseEntity<Page<ProductResponse>> listDeletedProducts(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        var page = productService.listDeletedProducts(pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Get product by id")
    @ApiResponse(responseCode = "200", description = "Product found")
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable @NonNull Long id) {
        var product = productService.getProductById(id);
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @Operation(summary = "Update product")
    @ApiResponse(responseCode = "200", description = "Product updated")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable @NonNull Long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        var product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @Operation(summary = "Deactivate product")
    @ApiResponse(responseCode = "200", description = "Product deactivated")
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ProductResponse> deactivateProduct(@PathVariable @NonNull Long id) {
        var product = productService.deactivateProduct(id);
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @Operation(summary = "Activate product")
    @ApiResponse(responseCode = "200", description = "Product activated")
    @PostMapping("/{id}/activate")
    public ResponseEntity<ProductResponse> activateProduct(@PathVariable @NonNull Long id) {
        var product = productService.activateProduct(id);
        return ResponseEntity.ok(ProductResponse.from(product));
    }

}
