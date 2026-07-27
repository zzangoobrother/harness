package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 데이터 접근. category 필터를 위한 조회 메서드와
 * 주문 생성 시 사용할 원자적 재고 차감 메서드를 제공한다.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByCategory(String category, Pageable pageable);

    /**
     * 재고를 **조건부로** 차감하고 갱신된 행 수를 반환한다. 반환값 0 = 재고 부족(차감되지 않음).
     *
     * <p>왜 이 방식인가(동시성):
     * "엔티티를 읽어 stock 을 검사한 뒤 차감"하는 방식은 두 요청이 같은 재고를 동시에 읽으면
     * 둘 다 통과하여 재고가 음수로 내려가는 lost update 가 발생한다.
     * 여기서는 검사(`stock >= :quantity`)와 차감을 **DB 의 단일 UPDATE 문 안**에 넣어
     * 행 잠금이 걸린 상태에서 원자적으로 수행하므로, 경쟁하는 두 트랜잭션 중 하나만 1행을 갱신하고
     * 다른 하나는 0행을 갱신하게 되어 호출 측이 이를 OUT_OF_STOCK 으로 판정할 수 있다.
     *
     * <p>Stage 3 의 `CartService.insertNewItem` 처럼 "예외를 잡고 같은 트랜잭션에서 복구"하는 패턴은
     * JPA 가 트랜잭션을 rollback-only 로 마킹해 버려 의도대로 동작하지 않으므로 재고 차감에는 쓰지 않는다.
     *
     * <p>영속성 컨텍스트는 일부러 비우지 않는다(clearAutomatically 미사용). 이 UPDATE 이후 같은
     * 트랜잭션에서 Product.stock 을 다시 읽어 쓰는 코드가 없고, 엔티티를 자바 코드로 수정하지 않으므로
     * 커밋 시점에 오래된 stock 이 덮어쓰이지 않는다. 반면 컨텍스트를 비우면 함께 로딩해 둔
     * Cart/CartItem 이 준영속 상태가 되어 이후 삭제 처리가 번거로워진다.
     */
    @Modifying(flushAutomatically = true)
    @Transactional
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity "
            + "WHERE p.id = :productId AND p.stock >= :quantity")
    int decreaseStockIfAvailable(@Param("productId") Long productId, @Param("quantity") int quantity);
}
