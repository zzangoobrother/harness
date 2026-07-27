package com.example.ecommerce.order;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 주문/모의결제 엔드포인트 통합 테스트. 계약(api-spec.md §4·§5 / types.ts)의 응답 shape·상태코드와
 * 일치하는지 검증한다. H2 in-memory(test 프로파일)로 실제 DB 없이 실행하며,
 * 상품은 ProductRepository 로 직접 시드한다(ProductSeeder 는 "!test" 한정이라 test 프로파일에서는 비활성).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    private Long earphoneId;   // 재고 50, 129000원
    private Long sneakerId;    // 재고 3, 89000원

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        Product earphone = productRepository.save(
                new Product("무선 이어폰", "노이즈 캔슬링", 129000,
                        "https://picsum.photos/seed/order-1/400/400", 50, "electronics"));
        Product sneaker = productRepository.save(
                new Product("한정판 스니커즈", "재고 적음", 89000,
                        "https://picsum.photos/seed/order-2/400/400", 3, "fashion"));
        earphoneId = earphone.getId();
        sneakerId = sneaker.getId();
    }

    // ------------------------------------------------------------------
    // POST /api/orders — 체크아웃
    // ------------------------------------------------------------------

    @Test
    void checkout_returns201_withPriceSnapshotAndPaymentNull() throws Exception {
        String token = signupAndGetToken("checkout@example.com");
        addCartItem(token, earphoneId, 2);

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(258000))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").isNumber())
                .andExpect(jsonPath("$.items[0].productId").value(earphoneId))
                .andExpect(jsonPath("$.items[0].productName").value("무선 이어폰"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].priceAtOrder").value(129000))
                .andExpect(jsonPath("$.items[0].subtotal").value(258000))
                .andExpect(jsonPath("$.createdAt").isString())
                .andReturn();

        assertFieldIsPresentAndNull(result, "$", "payment");
    }

    @Test
    void checkout_emptiesCartAndDecreasesStock() throws Exception {
        String token = signupAndGetToken("checkout-side-effect@example.com");
        addCartItem(token, earphoneId, 2);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());

        // 장바구니가 비워졌는지
        mockMvc.perform(get("/api/cart").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalAmount").value(0));

        // 재고가 주문 생성 시점에 차감됐는지 (50 - 2 = 48)
        assertThat(productRepository.findById(earphoneId).orElseThrow().getStock()).isEqualTo(48);
    }

    @Test
    void checkout_emptyCart_returns409CartEmpty() throws Exception {
        String token = signupAndGetToken("emptycart@example.com");

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CART_EMPTY"));
    }

    @Test
    void checkout_insufficientStock_returns409AndDecreasesNothing() throws Exception {
        String token = signupAndGetToken("outofstock@example.com");
        // 재고가 충분한 시점에 두 상품을 담아 두고,
        addCartItem(token, earphoneId, 1);
        addCartItem(token, sneakerId, 3);
        // 체크아웃 직전에 스니커즈 재고가 다른 경로로 소진된 상황을 만든다(3 → 1).
        assertThat(productRepository.decreaseStockIfAvailable(sneakerId, 2)).isEqualTo(1);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OUT_OF_STOCK"));

        // all-or-nothing: 먼저 처리된 이어폰 재고도 차감되지 않아야 한다.
        assertThat(productRepository.findById(earphoneId).orElseThrow().getStock()).isEqualTo(50);
        assertThat(productRepository.findById(sneakerId).orElseThrow().getStock()).isEqualTo(1);
    }

    @Test
    void checkout_withNoBodyAtAll_returns201() throws Exception {
        String token = signupAndGetToken("checkout-nobody@example.com");
        addCartItem(token, earphoneId, 1);

        // 계약상 본문은 빈 객체 {} 이며 사용하는 필드가 없다.
        // 본문을 생략한 클라이언트도 400 이 아니라 정상 생성되어야 한다(@RequestBody required=false).
        mockMvc.perform(post("/api/orders").header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(129000));
    }

    @Test
    void checkout_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // ------------------------------------------------------------------
    // GET /api/orders — 내 주문 목록
    // ------------------------------------------------------------------

    @Test
    void listOrders_returnsArrayInLatestFirstOrder() throws Exception {
        String token = signupAndGetToken("list@example.com");
        addCartItem(token, earphoneId, 1);
        long firstOrderId = checkout(token);
        addCartItem(token, sneakerId, 1);
        long secondOrderId = checkout(token);

        // PageResponse 가 아니라 배열이어야 한다(계약 가정 D).
        MvcResult result = mockMvc.perform(get("/api/orders").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(secondOrderId))
                .andExpect(jsonPath("$[1].id").value(firstOrderId))
                .andExpect(jsonPath("$[0].items[0].productName").value("한정판 스니커즈"))
                .andReturn();

        assertFieldIsPresentAndNull(result, "$[0]", "payment");
    }

    @Test
    void listOrders_withNoOrders_returnsEmptyArray() throws Exception {
        String token = signupAndGetToken("emptylist@example.com");

        mockMvc.perform(get("/api/orders").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ------------------------------------------------------------------
    // GET /api/orders/{id} — 주문 상세
    // ------------------------------------------------------------------

    @Test
    void getOrder_returns200_withSameShapeAsCheckout() throws Exception {
        String token = signupAndGetToken("detail@example.com");
        addCartItem(token, earphoneId, 2);
        long orderId = checkout(token);

        MvcResult result = mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(258000))
                .andExpect(jsonPath("$.items[0].priceAtOrder").value(129000))
                .andExpect(jsonPath("$.items[0].subtotal").value(258000))
                .andReturn();

        assertFieldIsPresentAndNull(result, "$", "payment");
    }

    @Test
    void getOrder_otherUsersOrder_returns403() throws Exception {
        String ownerToken = signupAndGetToken("owner-order@example.com");
        addCartItem(ownerToken, earphoneId, 1);
        long orderId = checkout(ownerToken);

        String strangerToken = signupAndGetToken("stranger-order@example.com");
        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(strangerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getOrder_notFound_returns404() throws Exception {
        String token = signupAndGetToken("order-notfound@example.com");

        mockMvc.perform(get("/api/orders/999999").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // ------------------------------------------------------------------
    // POST /api/orders/{id}/payment — 모의결제
    // ------------------------------------------------------------------

    @Test
    void pay_success_returns200_andMarksOrderPaid() throws Exception {
        String token = signupAndGetToken("pay-success@example.com");
        addCartItem(token, earphoneId, 2);
        long orderId = checkout(token);

        mockMvc.perform(post("/api/orders/" + orderId + "/payment")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("simulateSuccess", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.amount").value(258000))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.method").value("MOCK"))
                .andExpect(jsonPath("$.paidAt").isString());

        // 주문 상세에도 PAID 와 결제 정보가 반영된다.
        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.payment.status").value("SUCCESS"))
                .andExpect(jsonPath("$.payment.orderId").value(orderId));
    }

    @Test
    void pay_withEmptyJsonObject_defaultsToSuccess() throws Exception {
        String token = signupAndGetToken("pay-default@example.com");
        addCartItem(token, earphoneId, 1);
        long orderId = checkout(token);

        // 프론트가 { simulateSuccess: undefined } 를 보내면 실제 전송 본문은 {} 가 된다.
        // 계약상 생략은 true(성공)이므로, 래퍼 타입 Boolean 으로 받아 null 을 true 로 해석해야 한다.
        // primitive boolean(기본값 false)으로 받으면 이 테스트가 FAILED 로 뒤집힌다.
        mockMvc.perform(post("/api/orders/" + orderId + "/payment")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void pay_withNoBodyAtAll_defaultsToSuccess() throws Exception {
        String token = signupAndGetToken("pay-nobody@example.com");
        addCartItem(token, earphoneId, 1);
        long orderId = checkout(token);

        // 본문을 아예 보내지 않아도 400 이 아니라 성공(true)으로 처리되어야 한다(@RequestBody required=false).
        mockMvc.perform(post("/api/orders/" + orderId + "/payment")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void pay_failure_returns200WithFailedStatus_notAnError() throws Exception {
        String token = signupAndGetToken("pay-failed@example.com");
        addCartItem(token, earphoneId, 2);
        long orderId = checkout(token);

        // 결제 실패는 에러가 아니다: HTTP 200 + 본문 status=FAILED, paidAt=null.
        MvcResult result = mockMvc.perform(post("/api/orders/" + orderId + "/payment")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("simulateSuccess", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.amount").value(258000))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.method").value("MOCK"))
                .andReturn();

        assertFieldIsPresentAndNull(result, "$", "paidAt");

        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.payment.status").value("FAILED"));
    }

    @Test
    void pay_alreadyPaidOrder_returns409() throws Exception {
        String token = signupAndGetToken("pay-twice@example.com");
        addCartItem(token, earphoneId, 1);
        long orderId = checkout(token);

        mockMvc.perform(post("/api/orders/" + orderId + "/payment")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(post("/api/orders/" + orderId + "/payment")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_PAID"));
    }

    @Test
    void pay_failedOrder_canBeRetried() throws Exception {
        String token = signupAndGetToken("pay-retry@example.com");
        addCartItem(token, earphoneId, 1);
        long orderId = checkout(token);

        mockMvc.perform(post("/api/orders/" + orderId + "/payment")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("simulateSuccess", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));

        // 실패한 주문은 409 가 아니라 재결제가 가능해야 하며, Payment 는 주문당 1건이므로 갱신된다.
        mockMvc.perform(post("/api/orders/" + orderId + "/payment")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("simulateSuccess", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.paidAt").isString());

        mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void pay_otherUsersOrder_returns403() throws Exception {
        String ownerToken = signupAndGetToken("owner-pay@example.com");
        addCartItem(ownerToken, earphoneId, 1);
        long orderId = checkout(ownerToken);

        String strangerToken = signupAndGetToken("stranger-pay@example.com");
        mockMvc.perform(post("/api/orders/" + orderId + "/payment")
                        .header("Authorization", bearer(strangerToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void pay_orderNotFound_returns404() throws Exception {
        String token = signupAndGetToken("pay-notfound@example.com");

        mockMvc.perform(post("/api/orders/999999/payment")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // ------------------------------------------------------------------
    // 헬퍼
    // ------------------------------------------------------------------

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

    private void addCartItem(String token, Long productId, int quantity) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", quantity));
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    /**
     * 해당 필드가 응답 JSON 에 **존재하면서** 값이 null 인지 확인한다.
     * jsonPath 의 doesNotExist() 는 "필드 자체가 없는 경우"도 통과시켜 계약 위반(필드 누락)을 놓치므로
     * 원문을 직접 파싱해 존재 여부와 null 여부를 나눠 검증한다.
     *
     * @param parentPath 검사 대상의 부모 위치. 최상위면 "$", 배열 첫 요소면 "$[0]".
     */
    private void assertFieldIsPresentAndNull(MvcResult result, String parentPath, String fieldName)
            throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode parent = "$".equals(parentPath) ? root : root.get(Integer.parseInt(
                parentPath.substring(parentPath.indexOf('[') + 1, parentPath.indexOf(']'))));

        assertThat(parent.has(fieldName))
                .as("%s 필드가 응답에 존재해야 한다(계약이 null 을 명시한 필드는 생략하지 않는다)", fieldName)
                .isTrue();
        assertThat(parent.get(fieldName).isNull())
                .as("%s 는 null 이어야 한다", fieldName)
                .isTrue();
    }

    /** 체크아웃 후 생성된 주문 id 를 반환한다. */
    private long checkout(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }
}
