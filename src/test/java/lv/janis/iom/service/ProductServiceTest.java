package lv.janis.iom.service;

import jakarta.persistence.EntityNotFoundException;
import lv.janis.iom.dto.requests.ProductCreationRequest;
import lv.janis.iom.dto.requests.ProductUpdateRequest;
import lv.janis.iom.dto.filters.ListProductFilter;
import lv.janis.iom.dto.response.ProductResponse;
import lv.janis.iom.entity.Product;
import lv.janis.iom.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
  @Mock
  ProductRepository productRepository;
  @InjectMocks
  ProductService productService;

  @Test
  void createProduct_nullRequest_throws() {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> productService.createProduct(null));
    assertEquals("request is required", ex.getMessage());
    verifyNoInteractions(productRepository);
  }

  @SuppressWarnings("null")
  @Test
  void createProduct_skuExists_throws_andDoesNotSave() {
    var req = new ProductCreationRequest();
    req.setSku("SKU-1");
    req.setName("Name1");
    req.setDescription("desc");
    req.setPrice(new BigDecimal("9.99"));

    when(productRepository.existsBySku("SKU-1")).thenReturn(true);

    var ex = assertThrows(IllegalStateException.class,
        () -> productService.createProduct(req));

    assertTrue(ex.getMessage().contains("SKU-1"));
    verify(productRepository).existsBySku("SKU-1");
    verify(productRepository, never()).save(any());
    verify(productRepository, never()).existsByName(any());
  }

  @SuppressWarnings("null")
  @Test
  void createProduct_nameExists_throws_andDoesNotSave() {
    var req = new ProductCreationRequest();
    req.setSku("SKU-1");
    req.setName("Name1");
    req.setDescription("desc");
    req.setPrice(new BigDecimal("9.99"));

    when(productRepository.existsBySku("SKU-1")).thenReturn(false);
    when(productRepository.existsByName("Name1")).thenReturn(true);

    var ex = assertThrows(IllegalStateException.class,
        () -> productService.createProduct(req));

    assertTrue(ex.getMessage().contains("Name1"));
    verify(productRepository).existsBySku("SKU-1");
    verify(productRepository).existsByName("Name1");
    verify(productRepository, never()).save(any());
  }

  @SuppressWarnings("null")
  @Test
  void createProduct_ok_savesAndReturns() {
    var req = new ProductCreationRequest();
    req.setSku("SKU-1");
    req.setName("Name1");
    req.setDescription("desc");
    req.setPrice(new BigDecimal("9.99"));

    when(productRepository.existsBySku("SKU-1")).thenReturn(false);
    when(productRepository.existsByName("Name1")).thenReturn(false);

    var saved = Product.create("SKU-1", "Name1", "desc", new BigDecimal("9.99"));
    when(productRepository.save(any(Product.class))).thenReturn(saved);

    var result = productService.createProduct(req);

    assertSame(saved, result);

    var captor = ArgumentCaptor.forClass(Product.class);
    verify(productRepository).save(captor.capture());
    assertEquals("SKU-1", captor.getValue().getSku());
    assertEquals("Name1", captor.getValue().getName());
    assertEquals("desc", captor.getValue().getDescription());
    assertEquals(new BigDecimal("9.99"), captor.getValue().getPrice());
  }

  @Test
  void getProductById_throws_missing() {
    when(productRepository.findById(1L)).thenReturn(Optional.empty());
    var ex = assertThrows(EntityNotFoundException.class,
        () -> productService.getProductById(1L));

    assertEquals("Product not found.", ex.getMessage());
  }

  @Test
  void getProductById_returns_product() {
    Long productId = 1L;

    Product product = mock(Product.class);
    when(productRepository.findById(productId)).thenReturn(Optional.of(product));

    Product result = productService.getProductById(productId);

    assertSame(product, result);
    verify(productRepository).findById(productId);
  }

  @Test
  void updateProduct_nullRequest_throws() {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> productService.updateProduct(1L, null));
    assertEquals("request is required", ex.getMessage());
    verifyNoInteractions(productRepository);
  }

  @Test
  void updateProduct_noUpdates_throws() {
    var req = new ProductUpdateRequest();

    var ex = assertThrows(IllegalStateException.class,
        () -> productService.updateProduct(1L, req));
    assertEquals("At least one field must be provided for update", ex.getMessage());
    verifyNoInteractions(productRepository);
  }

  @Test
  void updateProduct_missingProduct_throws() {
    var req = new ProductUpdateRequest();
    req.setName("New Name");

    when(productRepository.findById(1L)).thenReturn(Optional.empty());

    var ex = assertThrows(EntityNotFoundException.class,
        () -> productService.updateProduct(1L, req));

    assertEquals("Product not found.", ex.getMessage());
    verify(productRepository).findById(1L);
    verify(productRepository, never()).save(any());
  }

  @Test
  void updateProduct_nameConflict_throws_andDoesNotSave() {
    var req = new ProductUpdateRequest();
    req.setName("New Name");

    var product = spy(Product.create("SKU-1", "Old Name", "desc", new BigDecimal("9.99")));
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(productRepository.existsByName("New Name")).thenReturn(true);

    var ex = assertThrows(IllegalStateException.class,
        () -> productService.updateProduct(1L, req));

    assertTrue(ex.getMessage().contains("New Name"));
    verify(productRepository).findById(1L);
    verify(productRepository).existsByName("New Name");
    verify(productRepository, never()).save(any());
    verify(product, never()).rename(any());
  }

  @Test
  void updateProduct_sameName_doesNotCheckNameExists() {
    var req = new ProductUpdateRequest();
    req.setName("Same Name");

    var product = spy(Product.create("SKU-1", "Same Name", "desc", new BigDecimal("9.99")));
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(productRepository.save(any(Product.class))).thenReturn(product);

    var result = productService.updateProduct(1L, req);

    assertSame(product, result);
    verify(productRepository).findById(1L);
    verify(productRepository, never()).existsByName(any());
    verify(productRepository).save(product);
  }

  @Test
  void updateProduct_updatesFields_andSaves() {
    var req = new ProductUpdateRequest();
    req.setName("New Name");
    req.setDescription("New Desc");
    req.setPrice(new BigDecimal("19.99"));

    var product = Product.create("SKU-1", "Old Name", "desc", new BigDecimal("9.99"));
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(productRepository.existsByName("New Name")).thenReturn(false);
    when(productRepository.save(any(Product.class))).thenReturn(product);

    var result = productService.updateProduct(1L, req);

    assertSame(product, result);
    assertEquals("New Name", product.getName());
    assertEquals("New Desc", product.getDescription());
    assertEquals(new BigDecimal("19.99"), product.getPrice());
    verify(productRepository).existsByName("New Name");
    verify(productRepository).save(product);
  }

  @Test
  void deactivateProduct_missingProduct_throws() {
    when(productRepository.findById(1L)).thenReturn(Optional.empty());

    var ex = assertThrows(EntityNotFoundException.class,
        () -> productService.deactivateProduct(1L));

    assertEquals("Product not found.", ex.getMessage());
    verify(productRepository).findById(1L);
    verify(productRepository, never()).save(any());
  }

  @Test
  void deactivateProduct_ok_updatesAndSaves() {
    var product = spy(Product.create("SKU-1", "Name1", "desc", new BigDecimal("9.99")));
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(productRepository.save(any(Product.class))).thenReturn(product);

    var result = productService.deactivateProduct(1L);

    assertSame(product, result);
    assertTrue(product.isDeleted());
    verify(product).deactivate();
    verify(productRepository).save(product);
  }

  @Test
  void activateProduct_missingProduct_throws() {
    when(productRepository.findById(1L)).thenReturn(Optional.empty());

    var ex = assertThrows(EntityNotFoundException.class,
        () -> productService.activateProduct(1L));

    assertEquals("Product not found.", ex.getMessage());
    verify(productRepository).findById(1L);
    verify(productRepository, never()).save(any());
  }

  @Test
  void activateProduct_ok_updatesAndSaves() {
    var product = spy(Product.create("SKU-1", "Name1", "desc", new BigDecimal("9.99")));
    product.deactivate();
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));
    when(productRepository.save(any(Product.class))).thenReturn(product);

    var result = productService.activateProduct(1L);

    assertSame(product, result);
    assertFalse(product.isDeleted());
    verify(product).activate();
    verify(productRepository).save(product);
  }

  @Test
  void listProducts_nullFilter_capsPageSize_andMaps() {
    var sort = Sort.by("name").ascending();
    Pageable pageable = PageRequest.of(1, 250, sort);

    var products = List.of(
        Product.create("SKU-1", "Name1", "desc1", new BigDecimal("9.99")),
        Product.create("SKU-2", "Name2", "desc2", new BigDecimal("19.99")));
    Page<Product> page = new PageImpl<>(products, PageRequest.of(1, 100, sort), 2);
    when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

    Page<ProductResponse> result = productService.listProducts(null, pageable);

    assertEquals(2, result.getContent().size());
    assertEquals("SKU-1", result.getContent().get(0).sku());
    assertEquals("SKU-2", result.getContent().get(1).sku());

    var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(productRepository).findAll(any(Specification.class), pageableCaptor.capture());
    assertEquals(1, pageableCaptor.getValue().getPageNumber());
    assertEquals(100, pageableCaptor.getValue().getPageSize());
    assertEquals(sort, pageableCaptor.getValue().getSort());
  }

  @Test
  void listProducts_withFilter_keepsPageSize() {
    Pageable pageable = PageRequest.of(0, 50, Sort.by("sku").descending());
    Page<Product> page = new PageImpl<>(List.of(), pageable, 0);
    when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

    var filter = new ListProductFilter();
    filter.setQuery("steel");

    Page<ProductResponse> result = productService.listProducts(filter, pageable);

    assertSame(pageable, result.getPageable());
    verify(productRepository).findAll(any(Specification.class), eq(pageable));
  }

  @Test
  void listDeletedProducts_capsPageSize() {
    Pageable pageable = PageRequest.of(2, 1000, Sort.by("sku"));
    Page<Product> page = new PageImpl<>(List.of(), PageRequest.of(2, 100, Sort.by("sku")), 0);
    when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

    Page<ProductResponse> result = productService.listDeletedProducts(pageable);

    assertEquals(0, result.getTotalElements());
    var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(productRepository).findAll(any(Specification.class), pageableCaptor.capture());
    assertEquals(2, pageableCaptor.getValue().getPageNumber());
    assertEquals(100, pageableCaptor.getValue().getPageSize());
  }
}
