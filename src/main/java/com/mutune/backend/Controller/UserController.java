package com.mutune.backend.Controller;

import com.mutune.backend.DTO.UserDTO;
import com.mutune.backend.Entity.AuthProvider;
import com.mutune.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 사용자 API 엔드포인트
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 소셜 최초 유입 or 기존 유저 반환 */
    @PostMapping("/social-register")
    public UserDTO socialRegister(
            @RequestParam AuthProvider provider,
            @RequestParam String providerId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String profileImage
    ) {
        return userService.registerOrGet(provider, providerId, email, username, profileImage);
    }

    @GetMapping("/{id}")
    public UserDTO get(@PathVariable Long id) {
        return userService.getById(id).orElse(null);
    }

    @GetMapping
    public List<UserDTO> list() {
        return userService.listAll();
    }

    @PatchMapping("/{id}/complete")
    public UserDTO complete(@PathVariable Long id) {
        return userService.markSignupComplete(id);
    }

    @PostMapping("/{id}/use-free")
    public UserDTO useFree(@PathVariable Long id) {
        return userService.useFreeOnce(id);
    }

    @PatchMapping("/{id}/agree-terms")
    public UserDTO agreeToTerms(@PathVariable Long id) {
        return userService.agreeToTerms(id);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }


}
