package com.example.ecommerce.auth.service;

import com.example.ecommerce.auth.dto.AuthResponse;
import com.example.ecommerce.auth.dto.LoginRequest;
import com.example.ecommerce.auth.dto.SignupRequest;
import com.example.ecommerce.common.exception.DuplicateEmailException;
import com.example.ecommerce.common.exception.InvalidCredentialsException;
import com.example.ecommerce.config.security.JwtTokenProvider;
import com.example.ecommerce.user.dto.UserResponse;
import com.example.ecommerce.user.entity.User;
import com.example.ecommerce.user.entity.UserRole;
import com.example.ecommerce.user.mapper.UserMapper;
import com.example.ecommerce.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 비즈니스 로직: 회원가입/로그인.
 * 비밀번호는 BCrypt 로 해싱 저장하고, 성공 시 JWT + UserResponse 를 담은 AuthResponse 를 반환한다.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    /** 회원가입: 이메일 중복 시 409 EMAIL_DUPLICATED. */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                UserRole.USER
        );
        User saved = userRepository.save(user);
        return toAuthResponse(saved);
    }

    /** 로그인: 이메일 미존재 또는 비밀번호 불일치 시 401 INVALID_CREDENTIALS. */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = tokenProvider.createToken(user);
        UserResponse userResponse = UserMapper.toResponse(user);
        return new AuthResponse(token, userResponse);
    }
}
