// 표시용 포맷 헬퍼. 주문/결제 화면이 공통으로 사용한다.
// (기존 상품/장바구니 페이지는 각자 로컬 formatPrice를 갖고 있으나, 이번 단계에서 추가되는
//  화면이 3개라 공용 모듈로 분리했다. 기존 페이지는 건드리지 않는다.)

/** 금액 포맷. 계약상 금액은 정수 원(KRW)이므로 소수점 없이 천단위 구분만 적용한다. */
export function formatPrice(price: number): string {
  return `${price.toLocaleString("ko-KR")}원`;
}

/** ISO 8601 문자열(계약의 createdAt/paidAt)을 한국어 날짜·시간 표기로 변환한다. */
export function formatDateTime(iso: string): string {
  const date = new Date(iso);
  // 서버가 예기치 않은 형식을 보냈을 때 "Invalid Date"를 노출하지 않고 원문을 그대로 보여준다.
  if (Number.isNaN(date.getTime())) return iso;
  return date.toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}
