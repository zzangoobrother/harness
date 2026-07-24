package com.example.ecommerce.product.config;

import com.example.ecommerce.product.entity.Product;
import com.example.ecommerce.product.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * 개발 환경(로컬 PostgreSQL)에서 상품 목록이 비어 있지 않도록 넣는 시드 데이터.
 * test 프로필(H2)에서는 각 테스트가 독립된 스키마(create-drop)로 시작해야 하므로
 * 시드가 개입하지 않도록 "!test" 로 한정한다. 이미 데이터가 있으면(재시작 등) 중복 삽입하지 않는다.
 */
@Configuration
@Profile("!test")
public class ProductSeeder {

    @Bean
    CommandLineRunner seedProducts(ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() > 0) {
                return;
            }

            List<Product> seed = List.of(
                    new Product("무선 이어폰", "노이즈 캔슬링 지원 블루투스 이어폰", 129000,
                            "https://picsum.photos/seed/product-1/400/400", 50, "electronics"),
                    new Product("스마트워치", "심박수·수면 측정 기능을 갖춘 스마트워치", 259000,
                            "https://picsum.photos/seed/product-2/400/400", 30, "electronics"),
                    new Product("블루투스 스피커", "휴대용 방수 블루투스 스피커", 89000,
                            "https://picsum.photos/seed/product-3/400/400", 40, "electronics"),
                    new Product("노트북 거치대", "높이 조절이 가능한 알루미늄 노트북 거치대", 35000,
                            "https://picsum.photos/seed/product-4/400/400", 60, "electronics"),
                    new Product("캐주얼 후드티", "기모 안감의 겨울용 캐주얼 후드티", 49000,
                            "https://picsum.photos/seed/product-5/400/400", 100, "fashion"),
                    new Product("데님 청바지", "스트레이트 핏 기본 데님 청바지", 59000,
                            "https://picsum.photos/seed/product-6/400/400", 80, "fashion"),
                    new Product("가죽 크로스백", "천연 가죽 소재의 미니 크로스백", 89000,
                            "https://picsum.photos/seed/product-7/400/400", 45, "fashion"),
                    new Product("스니커즈", "쿠션감이 좋은 데일리 스니커즈", 79000,
                            "https://picsum.photos/seed/product-8/400/400", 70, "fashion"),
                    new Product("아로마 디퓨저", "은은한 향으로 공간을 채우는 초음파 디퓨저", 32000,
                            "https://picsum.photos/seed/product-9/400/400", 55, "home"),
                    new Product("극세사 담요", "부드러운 극세사 소재의 사계절 담요", 25000,
                            "https://picsum.photos/seed/product-10/400/400", 90, "home"),
                    new Product("스탠드 조명", "은은한 무드등 겸용 LED 스탠드 조명", 42000,
                            "https://picsum.photos/seed/product-11/400/400", 35, "home"),
                    new Product("정리수납 박스 세트", "다용도 접이식 정리수납 박스 3종 세트", 18000,
                            "https://picsum.photos/seed/product-12/400/400", 120, "home")
            );

            productRepository.saveAll(seed);
        };
    }
}
