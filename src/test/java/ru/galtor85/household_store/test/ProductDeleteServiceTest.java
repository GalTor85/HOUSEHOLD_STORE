package ru.galtor85.household_store.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.galtor85.household_store.entity.product.*;
import ru.galtor85.household_store.entity.user.User;
import ru.galtor85.household_store.repository.product.ProductAttributeRepository;
import ru.galtor85.household_store.repository.product.ProductMediaRepository;
import ru.galtor85.household_store.repository.product.ProductRepository;
import ru.galtor85.household_store.repository.product.ProductStockRepository;
import ru.galtor85.household_store.repository.user.UserRepository;
import ru.galtor85.household_store.service.manager.ManagerProductService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Real Service Delete Tests")
class ProductDeleteServiceTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductAttributeRepository productAttributeRepository;

    @Autowired
    private ProductMediaRepository productMediaRepository;

    @Autowired
    private ProductStockRepository productStockRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ManagerProductService managerProductService;

    private Long testProductId;
    private Long testAttributeId;
    private Long testMediaId;
    private Long testStockId;
    private User adminUser;

    @BeforeEach
    void setUp() {
        log.info("=== SETUP: Creating test data ===");

        adminUser = User.builder()
                .email("admin-delete-" + System.currentTimeMillis() + "@example.com")
                .firstName("Admin")
                .lastName("Delete")
                .mobileNumber("+79990000000")
                .creator("system")
                .build();
        adminUser = userRepository.save(adminUser);
        log.info("Created admin user with ID: {}", adminUser.getId());

        Product product = Product.builder()
                .sku("TEST-DELETE-" + System.currentTimeMillis())
                .name("Test Delete Product")
                .price(BigDecimal.valueOf(100))
                .active(true)
                .createdBy("test")
                .build();
        product = productRepository.save(product);
        testProductId = product.getId();
        log.info("Created product with ID: {}, SKU: {}", testProductId, product.getSku());

        ProductAttribute attribute = ProductAttribute.builder()
                .product(product)
                .name("Color")
                .value("Red")
                .build();
        product.getAttributes().add(attribute);
        product = productRepository.save(product);
        testAttributeId = product.getAttributes().getFirst().getId();
        log.info("Created attribute with ID: {}", testAttributeId);

        ProductMedia media = ProductMedia.builder()
                .productId(product.getId())
                .fileName("test-image.jpg")
                .filePath("/uploads/test-image.jpg")
                .mediaType(MediaType.IMAGE)
                .build();
        media = productMediaRepository.save(media);
        testMediaId = media.getId();
        log.info("Created media with ID: {}", testMediaId);

        ProductStock stock = ProductStock.builder()
                .productId(product.getId())
                .warehouseId(1L)
                .quantity(100)
                .build();
        stock = productStockRepository.save(stock);
        testStockId = stock.getId();
        log.info("Created stock with ID: {}", testStockId);

        log.info("=== SETUP COMPLETE ===");
    }

    @Test
    @DisplayName("TEST-01: Delete product - product and attributes deleted")
    void testDeleteProductAndAttributes() {
        log.info("=== TEST-01 START ===");

        assertThat(productRepository.existsById(testProductId)).isTrue();
        assertThat(productAttributeRepository.existsById(testAttributeId)).isTrue();

        managerProductService.deleteProduct(testProductId, adminUser.getId());

        assertThat(productRepository.existsById(testProductId)).isFalse();
        assertThat(productAttributeRepository.existsById(testAttributeId)).isFalse();

        log.info("=== TEST-01 PASSED ===");
    }

    @Test
    @DisplayName("TEST-02: Delete product - media should also be deleted")
    void testDeleteProductMediaDeleted() {
        log.info("=== TEST-02 START ===");

        assertThat(productMediaRepository.existsById(testMediaId)).isTrue();

        managerProductService.deleteProduct(testProductId, adminUser.getId());

        assertThat(productMediaRepository.existsById(testMediaId)).isFalse();

        log.info("=== TEST-02 PASSED ===");
    }

    @Test
    @DisplayName("TEST-03: Delete product - stock should also be deleted")
    void testDeleteProductStockDeleted() {
        log.info("=== TEST-03 START ===");

        assertThat(productStockRepository.existsById(testStockId)).isTrue();

        managerProductService.deleteProduct(testProductId, adminUser.getId());

        assertThat(productStockRepository.existsById(testStockId)).isFalse();

        log.info("=== TEST-03 PASSED ===");
    }
}