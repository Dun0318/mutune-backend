package com.mutune.backend.service;

import com.mutune.backend.DTO.UserDTO;
import com.mutune.backend.Entity.AuthProvider;
import com.mutune.backend.Entity.UserEntity;
import com.mutune.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 사용자 관련 핵심 로직 구현
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceIMP implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDTO registerOrGet(AuthProvider provider, String providerId, String email, String username, String profileImage) {
        // 1) provider + providerId로 먼저 조회
        Optional<UserEntity> existed = userRepository.findByProviderAndProviderId(provider, providerId);
        if (existed.isPresent()) {
            return toDTO(existed.get());
        }

        // 2) 신규 생성
        UserEntity created = UserEntity.builder()
                .provider(provider)
                .providerId(providerId)
                .email(email) // null 가능
                .username(username)
                .profileImage(profileImage)
                .signupComplete(false)
                .freeUsed(false)
                .build();

        return toDTO(userRepository.save(created));
    }

    @Override
    public Optional<UserDTO> getById(Long id) {
        return userRepository.findById(id).map(this::toDTO);
    }

    @Override
    public Optional<UserDTO> getByProviderAndProviderId(AuthProvider provider, String providerId) {
        return userRepository.findByProviderAndProviderId(provider, providerId).map(this::toDTO);
    }

    @Override
    public List<UserDTO> listAll() {
        return userRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDTO markSignupComplete(Long id) {
        UserEntity entity = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        entity.setSignupComplete(true);
        return toDTO(userRepository.save(entity));
    }

    @Override
    @Transactional
    public UserDTO useFreeOnce(Long id) {
        UserEntity entity = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        if (entity.isFreeUsed()) {
            return toDTO(entity); // 이미 사용함
        }
        entity.setFreeUsed(true); // 최초 무료 소진
        return toDTO(userRepository.save(entity));
    }

    /* DTO 변환 */
    private UserDTO toDTO(UserEntity e) {
        return UserDTO.builder()
                .id(e.getId())
                .email(e.getEmail())
                .provider(e.getProvider())
                .providerId(e.getProviderId())
                .username(e.getUsername())
                .profileImage(e.getProfileImage())
                .signupComplete(e.isSignupComplete())
                .freeUsed(e.isFreeUsed())
                .termsAgreed(e.isTermsAgreed())
                .build();
    }

    @Override
    @Transactional
    public UserDTO agreeToTerms(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setTermsAgreed(true);
        return toDTO(userRepository.save(user)); // 개인정보 약관 동의 여부
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }

}
