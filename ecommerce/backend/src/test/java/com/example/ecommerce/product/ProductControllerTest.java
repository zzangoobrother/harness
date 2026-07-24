package com.example.ecommerce.product;

import com.example.ecommerce.product.entity.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 상품 엔드포인트 통합 테스트. 계약(api-spec.md / types.ts)의 응답 shape·상태코드와 일치하는지 검증한다.
 * test 프로필에서는 시드 데이터가 비활성화되므로(ProductSeeder 는 "!test" 한정), 각 테스트가
 * 필요한 데이터를 직접 저장한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        productRepository.saveAll(java.util.List.of(
                new Product("무선 이어폰", "노이즈 캔슬링", 129000,
                        "https://picsum.photos/seed/test-1/400/400", 50, "electronics"),
                new Product("스마트워치", "심박수 측정", 259000,
                        "https://picsum.photos/seed/test-2/400/400", 30, "electronics"),
                new Product("캐주얼 후드티", "기모 안감", 49000,
                        "https://picsum.photos/seed/test-3/400/400", 100, "fashion")
        ));
    }

    @Test
    void list_returns200_withPageResponseShape() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.items[0].id").isNumber())
                .andExpect(jsonPath("$.items[0].name").isString())
                .andExpect(jsonPath("$.items[0].price").isNumber())
                .andExpect(jsonPath("$.items[0].stock").isNumber())
                .andExpect(jsonPath("$.items[0].createdAt").isString());
    }

    @Test
    void list_filtersByCategory() throws Exception {
        mockMvc.perform(get("/api/products").param("category", "fashion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].category").value("fashion"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void list_respectsPageAndSize() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void list_negativePage_returns400() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void list_zeroSize_returns400() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void list_nonNumericPage_returns400_notInternalError() throws Exception {
        // page=abc 는 int 로 바인딩 불가 → MethodArgumentTypeMismatchException.
        // GlobalExceptionHandler 가 전역으로 400 VALIDATION_ERROR 로 잡아야 하며, fallback(500)으로 새면 안 된다.
        mockMvc.perform(get("/api/products").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void detail_nonNumericId_returns400_notInternalError() throws Exception {
        // {id} 가 Long 으로 바인딩 불가한 경로 변수(abc) → 400 VALIDATION_ERROR, 500 금지.
        mockMvc.perform(get("/api/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void detail_returns200_withProductShape() throws Exception {
        Long id = productRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").isString())
                .andExpect(jsonPath("$.description").isString())
                .andExpect(jsonPath("$.price").isNumber())
                .andExpect(jsonPath("$.imageUrl").isString())
                .andExpect(jsonPath("$.stock").isNumber())
                .andExpect(jsonPath("$.category").isString())
                .andExpect(jsonPath("$.createdAt").isString());
    }

    @Test
    void detail_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/products/999999"));
    }

    @Test
    void list_isAccessibleWithoutAuthentication() throws Exception {
        // 인증 헤더 없이 호출해도 401이 아니라 200이어야 한다(permitAll 확인).
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }
}
