package com.example.ecommerce.product.service;

import com.example.ecommerce.common.dto.PageResponse;
import com.example.ecommerce.common.exception.InvalidPageRequestException;
import com.example.ecommerce.common.exception.ProductNotFoundException;
import com.example.ecommerce.product.dto.ProductResponse;
import com.example.ecommerce.product.entity.Product;
import com.example.ecommerce.product.mapper.ProductMapper;
import com.example.ecommerce.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 조회 비즈니스 로직: 목록(페이지네이션 + category 필터), 상세.
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /** 상품 목록. page&lt;0 또는 size&lt;=0 이면 400 VALIDATION_ERROR. */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> list(int page, int size, String category) {
        if (page < 0) {
            throw new InvalidPageRequestException("page는 0 이상이어야 합니다.");
        }
        if (size <= 0) {
            throw new InvalidPageRequestException("size는 1 이상이어야 합니다.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Product> products = (category != null && !category.isBlank())
                ? productRepository.findByCategory(category, pageable)
                : productRepository.findAll(pageable);

        return PageResponse.from(products.map(ProductMapper::toResponse));
    }

    /** 상품 상세. 없으면 404 NOT_FOUND. */
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("상품을 찾을 수 없습니다. id=" + id));
        return ProductMapper.toResponse(product);
    }
}
