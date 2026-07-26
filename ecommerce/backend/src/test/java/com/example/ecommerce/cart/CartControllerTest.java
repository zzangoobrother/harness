package com.example.ecommerce.cart;

import com.example.ecommerce.product.entity.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 장바구니 엔드포인트 통합 테스트. 계약(api-spec.md §3 / types.ts)의 응답 shape·상태코드와 일치하는지 검증한다.
 * H2 in-memory(test 프로파일)로 실제 DB 없이 실행하며, 상품은 ProductRepository 로 직접 시드한다
 * (ProductSeeder는 "!test" 한정이라 test 프로필에서는 비활성).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    private Long inStockProductId;
    private Long lowStockProductId;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        Product inStock = productRepository.save(
                new Product("무선 이어폰", "노이즈 캔슬링", 129000,
                        "https://picsum.photos/seed/cart-1/400/400", 50, "electronics"));
        Product lowStock = productRepository.save(
                new Product("한정판 스니커즈", "재고 적음", 89000,
                        "https://picsum.photos/seed/cart-2/400/400", 2, "fashion"));
        inStockProductId = inStock.getId();
        lowStockProductId = lowStock.getId();
    }

    private String signupAndGetToken(String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", email, "password", "secret123", "name", "테스터"));
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // ------------------------------------------------------------------
    // GET /api/cart
    // ------------------------------------------------------------------

    @Test
    void getCart_withoutExistingCart_lazilyCreatesEmptyCart() throws Exception {
        String token = signupAndGetToken("lazy@example.com");

        mockMvc.perform(get("/api/cart").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    void getCart_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // ------------------------------------------------------------------
    // POST /api/cart/items
    // ------------------------------------------------------------------

    @Test
    void addItem_returns201_withFullCartResponseShape() throws Exception {
        String token = signupAndGetToken("add@example.com");
        String body = objectMapper.writeValueAsString(Map.of("productId", inStockProductId, "quantity", 2));

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").isNumber())
                .andExpect(jsonPath("$.items[0].productId").value(inStockProductId))
                .andExpect(jsonPath("$.items[0].productName").value("무선 이어폰"))
                .andExpect(jsonPath("$.items[0].productImageUrl").value("https://picsum.photos/seed/cart-1/400/400"))
                .andExpect(jsonPath("$.items[0].price").value(129000))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].subtotal").value(258000))
                .andExpect(jsonPath("$.totalAmount").value(258000));
    }

    @Test
    void addItem_sameProductTwice_mergesQuantityIntoSingleRow() throws Exception {
        String token = signupAndGetToken("merge@example.com");
        String body = objectMapper.writeValueAsString(Map.of("productId", inStockProductId, "quantity", 2));

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(4))
                .andExpect(jsonPath("$.items[0].subtotal").value(516000))
                .andExpect(jsonPath("$.totalAmount").value(516000));
    }

    @Test
    void addItem_exceedsStock_returns409() throws Exception {
        String token = signupAndGetToken("stock@example.com");
        // lowStockProductId 의 재고는 2 → 3개 요청 시 초과
        String body = objectMapper.writeValueAsString(Map.of("productId", lowStockProductId, "quantity", 3));

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OUT_OF_STOCK"));
    }

    @Test
    void addItem_productNotFound_returns404() throws Exception {
        String token = signupAndGetToken("notfound@example.com");
        String body = objectMapper.writeValueAsString(Map.of("productId", 999999, "quantity", 1));

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void addItem_quantityLessThanOne_returns400() throws Exception {
        String token = signupAndGetToken("badqty@example.com");
        String body = objectMapper.writeValueAsString(Map.of("productId", inStockProductId, "quantity", 0));

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ------------------------------------------------------------------
    // PATCH /api/cart/items/{id}
    // ------------------------------------------------------------------

    @Test
    void updateItem_changesQuantity_returns200() throws Exception {
        String token = signupAndGetToken("update@example.com");
        Long itemId = addItemAndGetItemId(token, inStockProductId, 1);

        String body = objectMapper.writeValueAsString(Map.of("quantity", 5));
        mockMvc.perform(patch("/api/cart/items/" + itemId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.items[0].subtotal").value(645000))
                .andExpect(jsonPath("$.totalAmount").value(645000));
    }

    @Test
    void updateItem_exceedsStock_returns409() throws Exception {
        String token = signupAndGetToken("updatestock@example.com");
        Long itemId = addItemAndGetItemId(token, lowStockProductId, 1);

        String body = objectMapper.writeValueAsString(Map.of("quantity", 10));
        mockMvc.perform(patch("/api/cart/items/" + itemId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OUT_OF_STOCK"));
    }

    @Test
    void updateItem_quantityLessThanOne_returns400() throws Exception {
        String token = signupAndGetToken("updatebad@example.com");
        Long itemId = addItemAndGetItemId(token, inStockProductId, 1);

        String body = objectMapper.writeValueAsString(Map.of("quantity", 0));
        mockMvc.perform(patch("/api/cart/items/" + itemId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateItem_otherUsersItem_returns403() throws Exception {
        String ownerToken = signupAndGetToken("owner1@example.com");
        Long itemId = addItemAndGetItemId(ownerToken, inStockProductId, 1);

        String strangerToken = signupAndGetToken("stranger1@example.com");
        String body = objectMapper.writeValueAsString(Map.of("quantity", 2));
        mockMvc.perform(patch("/api/cart/items/" + itemId)
                        .header("Authorization", bearer(strangerToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void updateItem_notFound_returns404() throws Exception {
        String token = signupAndGetToken("updatenotfound@example.com");
        String body = objectMapper.writeValueAsString(Map.of("quantity", 2));
        mockMvc.perform(patch("/api/cart/items/999999")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // ------------------------------------------------------------------
    // DELETE /api/cart/items/{id}
    // ------------------------------------------------------------------

    @Test
    void removeItem_returns200_withoutRemovedItem() throws Exception {
        String token = signupAndGetToken("delete@example.com");
        Long itemId = addItemAndGetItemId(token, inStockProductId, 1);

        mockMvc.perform(delete("/api/cart/items/" + itemId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    void removeItem_otherUsersItem_returns403() throws Exception {
        String ownerToken = signupAndGetToken("owner2@example.com");
        Long itemId = addItemAndGetItemId(ownerToken, inStockProductId, 1);

        String strangerToken = signupAndGetToken("stranger2@example.com");
        mockMvc.perform(delete("/api/cart/items/" + itemId)
                        .header("Authorization", bearer(strangerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void removeItem_notFound_returns404() throws Exception {
        String token = signupAndGetToken("deletenotfound@example.com");
        mockMvc.perform(delete("/api/cart/items/999999")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // ------------------------------------------------------------------
    // 헬퍼
    // ------------------------------------------------------------------

    private Long addItemAndGetItemId(String token, Long productId, int quantity) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", quantity));
        MvcResult result = mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        Long itemId = json.get("items").get(0).get("id").asLong();
        assertThat(itemId).isNotNull();
        return itemId;
    }
}
