package com.example.ecommerce.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이지네이션 목록 응답 공통 DTO. 계약 types.ts 의 PageResponse&lt;T&gt; 와 필드명·순서 일치.
 * Spring Data 의 {@link Page} 를 그대로 직렬화하면 content/pageable/... 형태가 되어 계약을
 * 위반하므로, 반드시 이 DTO 로 변환한 뒤 응답한다.
 *
 * @param items          현재 페이지의 요소 목록
 * @param page           0-base 현재 페이지 번호
 * @param size           페이지 크기
 * @param totalElements  전체 요소 수
 * @param totalPages     전체 페이지 수
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
