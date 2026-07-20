package com.example.ecommerce.user.mapper;

import com.example.ecommerce.user.dto.UserResponse;
import com.example.ecommerce.user.entity.User;

/**
 * User 엔티티 → UserResponse 변환. passwordHash 를 응답에 싣지 않도록 이 매퍼만 통해 변환한다.
 */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
