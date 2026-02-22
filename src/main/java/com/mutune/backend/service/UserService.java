package com.mutune.backend.service;

import com.mutune.backend.DTO.UserDTO;
import com.mutune.backend.Entity.AuthProvider;

import java.util.List;
import java.util.Optional;

/**
 사용자 비즈니스 로직 정의
 */
public interface UserService {
    UserDTO registerOrGet(AuthProvider provider, String providerId, String email, String username, String profileImage);
    Optional<UserDTO> getById(Long id);
    Optional<UserDTO> getByProviderAndProviderId(AuthProvider provider, String providerId);
    List<UserDTO> listAll();
    UserDTO markSignupComplete(Long id);
    UserDTO useFreeOnce(Long id);
    UserDTO agreeToTerms(Long id);
    void deleteUser(Long id);

}
