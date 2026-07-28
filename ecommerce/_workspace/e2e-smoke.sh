#!/usr/bin/env bash
# 계약(_workspace/contract/api-spec.md) 기준 E2E 스모크.
# 실패해도 멈추지 않고 전 항목을 검사한 뒤 마지막에 요약한다.
BASE=http://localhost:8080
ORIGIN=http://localhost:5173
PASS=0; FAIL=0
declare -a FAILURES

ok()   { PASS=$((PASS+1)); printf '  PASS  %s\n' "$1"; }
bad()  { FAIL=$((FAIL+1)); FAILURES+=("$1 :: $2"); printf '  FAIL  %s\n        기대=%s\n' "$1" "$2"; }
chk()  { # chk <설명> <실제> <기대>
  if [ "$2" = "$3" ]; then ok "$1 ($2)"; else bad "$1" "$3, 실제=$2"; fi
}

# HTTP 요청 헬퍼: 본문을 BODY, 상태코드를 CODE 전역에 담는다.
req() { # req <METHOD> <PATH> [BODY_JSON] [AUTH_TOKEN]
  local m=$1 p=$2 b=$3 t=$4
  local args=(-s -o /tmp/_smoke_body -w '%{http_code}' -X "$m" "$BASE$p" -H "Origin: $ORIGIN")
  [ -n "$b" ] && args+=(-H 'Content-Type: application/json' -d "$b")
  [ -n "$t" ] && args+=(-H "Authorization: Bearer $t")
  CODE=$(curl "${args[@]}")
  BODY=$(cat /tmp/_smoke_body)
}

EMAIL="e2e-$(date +%s)@example.com"

echo "=== 1. 인증 ==========================================="
req POST /api/auth/signup "{\"email\":\"$EMAIL\",\"password\":\"secret123\",\"name\":\"E2E테스터\"}"
chk "1.1 signup 상태코드" "$CODE" "201"
TOKEN=$(jq -r '.token // empty' <<<"$BODY")
[ -n "$TOKEN" ] && ok "1.1 signup 토큰 발급" || bad "1.1 signup 토큰 발급" "token 존재, 실제본문=$BODY"
chk "1.1 signup user.role" "$(jq -r '.user.role // empty' <<<"$BODY")" "USER"
chk "1.1 signup user.email" "$(jq -r '.user.email // empty' <<<"$BODY")" "$EMAIL"
# createdAt 이 ISO 8601 문자열인지 (Jackson 3 타임스탬프 회귀 방지)
jq -e '.user.createdAt | type == "string"' <<<"$BODY" >/dev/null \
  && ok "1.1 createdAt ISO문자열 직렬화" || bad "1.1 createdAt ISO문자열 직렬화" "string, 실제=$(jq -c '.user.createdAt' <<<"$BODY")"

req POST /api/auth/signup "{\"email\":\"$EMAIL\",\"password\":\"secret123\",\"name\":\"중복\"}"
chk "1.1 이메일 중복 → 409" "$CODE" "409"
chk "1.1 중복 에러코드" "$(jq -r '.code // empty' <<<"$BODY")" "EMAIL_DUPLICATED"

req POST /api/auth/login "{\"email\":\"$EMAIL\",\"password\":\"secret123\"}"
chk "1.2 login 상태코드" "$CODE" "200"
req POST /api/auth/login "{\"email\":\"$EMAIL\",\"password\":\"wrongpass\"}"
chk "1.2 잘못된 비밀번호 → 401" "$CODE" "401"
chk "1.2 에러코드" "$(jq -r '.code // empty' <<<"$BODY")" "INVALID_CREDENTIALS"

echo "=== 2. 상품 ==========================================="
req GET /api/products
chk "2.1 목록 상태코드" "$CODE" "200"
# PageResponse shape (Spring Page 직렬화 누출 여부)
for f in items page size totalElements totalPages; do
  jq -e "has(\"$f\")" <<<"$BODY" >/dev/null && ok "2.1 PageResponse.$f 존재" || bad "2.1 PageResponse.$f 존재" "필드 존재"
done
jq -e 'has("pageable") or has("content")' <<<"$BODY" >/dev/null \
  && bad "2.1 Spring Page 누출 없음" "pageable/content 없어야 함" || ok "2.1 Spring Page 누출 없음"
SEED_COUNT=$(jq -r '.totalElements // 0' <<<"$BODY")
[ "$SEED_COUNT" -ge 1 ] && ok "2.1 시드 상품 존재 (totalElements=$SEED_COUNT)" || bad "2.1 시드 상품 존재" ">=1, 실제=$SEED_COUNT"

PID=$(jq -r '.items[0].id' <<<"$BODY")
PSTOCK=$(jq -r '.items[0].stock' <<<"$BODY")
PPRICE=$(jq -r '.items[0].price' <<<"$BODY")
jq -e '.items[0].price | type == "number"' <<<"$BODY" >/dev/null \
  && ok "2.1 price 정수(number) 타입" || bad "2.1 price 정수(number) 타입" "number"

req GET "/api/products?category=electronics"
chk "2.1 카테고리 필터 상태코드" "$CODE" "200"
jq -e '[.items[].category] | all(. == "electronics")' <<<"$BODY" >/dev/null \
  && ok "2.1 카테고리 필터 동작" || bad "2.1 카테고리 필터 동작" "전부 electronics"

req GET "/api/products/$PID"
chk "2.2 상세 상태코드" "$CODE" "200"
req GET "/api/products/999999"
chk "2.2 없는 상품 → 404" "$CODE" "404"
chk "2.2 에러코드" "$(jq -r '.code // empty' <<<"$BODY")" "NOT_FOUND"
req GET "/api/products/abc"
chk "2.2 타입 불일치 → 400 (Stage2 수정분)" "$CODE" "400"
chk "2.2 에러코드" "$(jq -r '.code // empty' <<<"$BODY")" "VALIDATION_ERROR"

echo "=== 3. 장바구니 ======================================="
req GET /api/cart
chk "3.1 미인증 → 401" "$CODE" "401"
chk "3.1 에러코드" "$(jq -r '.code // empty' <<<"$BODY")" "UNAUTHORIZED"

req GET /api/cart "" "$TOKEN"
chk "3.1 조회 상태코드(지연생성)" "$CODE" "200"
chk "3.1 빈 카트 items" "$(jq -r '.items | length' <<<"$BODY")" "0"
chk "3.1 빈 카트 totalAmount" "$(jq -r '.totalAmount' <<<"$BODY")" "0"

req POST /api/cart/items "{\"productId\":$PID,\"quantity\":2}" "$TOKEN"
chk "3.2 담기 상태코드(201)" "$CODE" "201"
ITEM_ID=$(jq -r '.items[0].id' <<<"$BODY")
chk "3.2 응답이 CartResponse 전체" "$(jq -r '.items | length' <<<"$BODY")" "1"
chk "3.2 subtotal 계산" "$(jq -r '.items[0].subtotal' <<<"$BODY")" "$((PPRICE*2))"
chk "3.2 totalAmount 계산" "$(jq -r '.totalAmount' <<<"$BODY")" "$((PPRICE*2))"

req POST /api/cart/items "{\"productId\":$PID,\"quantity\":1}" "$TOKEN"
chk "3.2 동일 상품 수량 합산" "$(jq -r '.items[0].quantity' <<<"$BODY")" "3"
chk "3.2 합산 후 아이템 1건 유지" "$(jq -r '.items | length' <<<"$BODY")" "1"

req POST /api/cart/items "{\"productId\":$PID,\"quantity\":0}" "$TOKEN"
chk "3.2 quantity<1 → 400" "$CODE" "400"
req POST /api/cart/items "{\"productId\":999999,\"quantity\":1}" "$TOKEN"
chk "3.2 없는 상품 → 404" "$CODE" "404"
req POST /api/cart/items "{\"productId\":$PID,\"quantity\":99999}" "$TOKEN"
chk "3.2 재고 초과 → 409" "$CODE" "409"
chk "3.2 에러코드" "$(jq -r '.code // empty' <<<"$BODY")" "OUT_OF_STOCK"

req PATCH "/api/cart/items/$ITEM_ID" '{"quantity":2}' "$TOKEN"
chk "3.3 수량 변경 상태코드" "$CODE" "200"
chk "3.3 수량 반영" "$(jq -r '.items[0].quantity' <<<"$BODY")" "2"

# 두 번째 상품 담고 삭제 검증
PID2=$(curl -s "$BASE/api/products" | jq -r '.items[1].id')
req POST /api/cart/items "{\"productId\":$PID2,\"quantity\":1}" "$TOKEN"
ITEM_ID2=$(jq -r --arg p "$PID2" '.items[] | select(.productId == ($p|tonumber)) | .id' <<<"$BODY")
chk "3.4 삭제 전 아이템 2건" "$(jq -r '.items | length' <<<"$BODY")" "2"
req DELETE "/api/cart/items/$ITEM_ID2" "" "$TOKEN"
chk "3.4 삭제 상태코드" "$CODE" "200"
chk "3.4 삭제 후 아이템 1건" "$(jq -r '.items | length' <<<"$BODY")" "1"

# 타인 소유 아이템 접근 → 403
OTHER="other-$(date +%s)@example.com"
req POST /api/auth/signup "{\"email\":\"$OTHER\",\"password\":\"secret123\",\"name\":\"타인\"}"
TOKEN2=$(jq -r '.token' <<<"$BODY")
req PATCH "/api/cart/items/$ITEM_ID" '{"quantity":1}' "$TOKEN2"
chk "3.3 타인 아이템 수정 → 403" "$CODE" "403"
chk "3.3 에러코드" "$(jq -r '.code // empty' <<<"$BODY")" "FORBIDDEN"

echo "=== 4. 주문 ==========================================="
STOCK_BEFORE=$(curl -s "$BASE/api/products/$PID" | jq -r '.stock')

req POST /api/orders '{}' "$TOKEN2"
chk "4.1 빈 장바구니 체크아웃 → 409" "$CODE" "409"
chk "4.1 에러코드" "$(jq -r '.code // empty' <<<"$BODY")" "CART_EMPTY"

req POST /api/orders '{}' "$TOKEN"
chk "4.1 체크아웃 상태코드(201)" "$CODE" "201"
ORDER_ID=$(jq -r '.id' <<<"$BODY")
chk "4.1 초기 status" "$(jq -r '.status' <<<"$BODY")" "PENDING"
chk "4.1 payment 초기 null" "$(jq -r '.payment' <<<"$BODY")" "null"
chk "4.1 totalAmount" "$(jq -r '.totalAmount' <<<"$BODY")" "$((PPRICE*2))"
chk "4.1 priceAtOrder 스냅샷" "$(jq -r '.items[0].priceAtOrder' <<<"$BODY")" "$PPRICE"
jq -e '.items[0] | has("productName")' <<<"$BODY" >/dev/null \
  && ok "4.1 OrderItem.productName 존재(types.ts 요구)" || bad "4.1 OrderItem.productName 존재" "필드 존재"

STOCK_AFTER=$(curl -s "$BASE/api/products/$PID" | jq -r '.stock')
chk "4.1 재고 차감(주문 시점)" "$STOCK_AFTER" "$((STOCK_BEFORE-2))"

req GET /api/cart "" "$TOKEN"
chk "4.1 주문 후 장바구니 비움" "$(jq -r '.items | length' <<<"$BODY")" "0"

req GET /api/orders "" "$TOKEN"
chk "4.2 목록 상태코드" "$CODE" "200"
jq -e 'type == "array"' <<<"$BODY" >/dev/null \
  && ok "4.2 응답이 배열(계약)" || bad "4.2 응답이 배열(계약)" "array, 실제=$(jq -r 'type' <<<"$BODY")"

req GET "/api/orders/$ORDER_ID" "" "$TOKEN"
chk "4.3 상세 상태코드" "$CODE" "200"
req GET "/api/orders/$ORDER_ID" "" "$TOKEN2"
chk "4.3 타인 주문 → 403" "$CODE" "403"
req GET "/api/orders/999999" "" "$TOKEN"
chk "4.3 없는 주문 → 404" "$CODE" "404"

echo "=== 5. 모의결제 ======================================="
req POST "/api/orders/$ORDER_ID/payment" '{"simulateSuccess":false}' "$TOKEN"
chk "5.1 결제실패도 HTTP 200(계약 가정7)" "$CODE" "200"
chk "5.1 payment.status=FAILED" "$(jq -r '.status' <<<"$BODY")" "FAILED"
chk "5.1 paidAt null" "$(jq -r '.paidAt' <<<"$BODY")" "null"
PAY_ID=$(jq -r '.id' <<<"$BODY")
req GET "/api/orders/$ORDER_ID" "" "$TOKEN"
chk "5.1 실패 후 주문 status=FAILED" "$(jq -r '.status' <<<"$BODY")" "FAILED"

STOCK_AFTER_FAIL=$(curl -s "$BASE/api/products/$PID" | jq -r '.stock')
chk "5.1 결제실패 시 재고 복원 안 함(설계결정)" "$STOCK_AFTER_FAIL" "$STOCK_AFTER"

# 본문 {} → simulateSuccess 기본 true (PROGRESS.md 회귀 위험 지점)
req POST "/api/orders/$ORDER_ID/payment" '{}' "$TOKEN"
chk "5.1 재결제(FAILED→재시도) 상태코드" "$CODE" "200"
chk "5.1 빈 본문 {} → 기본 SUCCESS" "$(jq -r '.status' <<<"$BODY")" "SUCCESS"
chk "5.1 Payment 행 재사용(같은 id)" "$(jq -r '.id' <<<"$BODY")" "$PAY_ID"
jq -e '.paidAt | type == "string"' <<<"$BODY" >/dev/null \
  && ok "5.1 성공 시 paidAt 설정" || bad "5.1 성공 시 paidAt 설정" "string"
chk "5.1 method" "$(jq -r '.method' <<<"$BODY")" "MOCK"

req GET "/api/orders/$ORDER_ID" "" "$TOKEN"
chk "5.1 성공 후 주문 status=PAID" "$(jq -r '.status' <<<"$BODY")" "PAID"

req POST "/api/orders/$ORDER_ID/payment" '{}' "$TOKEN"
chk "5.1 SUCCESS 재결제 → 409" "$CODE" "409"
chk "5.1 에러코드" "$(jq -r '.code // empty' <<<"$BODY")" "ALREADY_PAID"

req POST "/api/orders/$ORDER_ID/payment" '{}' "$TOKEN2"
chk "5.1 타인 주문 결제 → 403" "$CODE" "403"

echo "=== 6. 공통 에러 스키마 / CORS ========================"
req GET "/api/products/999999"
for f in code message timestamp status path; do
  jq -e "has(\"$f\")" <<<"$BODY" >/dev/null && ok "6 에러스키마.$f 존재" || bad "6 에러스키마.$f 존재" "필드 존재"
done

PREFLIGHT=$(curl -s -o /dev/null -w '%{http_code}' -X OPTIONS "$BASE/api/cart" \
  -H "Origin: $ORIGIN" -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: authorization,content-type")
[ "$PREFLIGHT" = "200" ] || [ "$PREFLIGHT" = "204" ] \
  && ok "6 CORS 프리플라이트 허용 ($PREFLIGHT)" || bad "6 CORS 프리플라이트 허용" "200/204, 실제=$PREFLIGHT"
ACAO=$(curl -s -D - -o /dev/null -X OPTIONS "$BASE/api/cart" -H "Origin: $ORIGIN" \
  -H "Access-Control-Request-Method: GET" | grep -i 'access-control-allow-origin' | tr -d '\r')
[ -n "$ACAO" ] && ok "6 $ACAO" || bad "6 Access-Control-Allow-Origin 헤더" "헤더 존재"

echo
echo "======================================================="
echo "  통과 $PASS / 실패 $FAIL"
if [ "$FAIL" -gt 0 ]; then
  echo "  --- 실패 목록 ---"
  for f in "${FAILURES[@]}"; do echo "  * $f"; done
fi
echo "======================================================="
exit 0
