package com.mutune.backend.Controller;

import com.mutune.backend.DTO.SheetMusicAfterResponseDTO;
import com.mutune.backend.Entity.SheetMusicAfterEntity;
import com.mutune.backend.Entity.SheetMusicBeforeEntity;
import com.mutune.backend.service.sheetMusic.SheetMusicAfterService;
import com.mutune.backend.service.sheetMusic.SheetMusicBeforeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
//@RestController
//@RequestMapping("/api/sheetmusic")
@RequiredArgsConstructor
public class SheetMusicController {

    private final SheetMusicBeforeService beforeService;
    private final SheetMusicAfterService afterService;

    // ==========================================================
    // 1) 원본 파일 업로드 (PDF / 이미지)
    // ==========================================================
    @PostMapping("/before")
    public ResponseEntity<?> uploadBefore(
            @RequestParam("file") List<MultipartFile> files,
            @RequestParam("userId") Long userId
    ) {
        try {
            List<SheetMusicBeforeEntity> savedFiles = new ArrayList<>();

            for (MultipartFile file : files) {
                savedFiles.add(beforeService.saveFile(file, userId));
            }

            return ResponseEntity.ok(savedFiles);

        } catch (Exception e) {
            log.error("원본 업로드 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("UPLOAD_FAILED");
        }
    }

    // ==========================================================
    // 2) 악보 변환 (Before → After)
    // ==========================================================
    @PostMapping("/convert/{beforeId}")
    public ResponseEntity<?> convertSheet(
            @PathVariable Long beforeId,
            @RequestParam("userId") Long userId
    ) {
        try {
            SheetMusicAfterResponseDTO dto =
                    afterService.convertSheet(beforeId, userId);

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            log.error("변환 실패", e);

            Map<String, Object> err = new HashMap<>();
            err.put("status", "CONVERSION_FAILED");
            err.put("message", e.getMessage());

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(err);
        }
    }

    // ==========================================================
    // 3) before 리스트 조회 (관리 / 디버그용)
    // ==========================================================
    @GetMapping("/before")
    public ResponseEntity<List<SheetMusicBeforeEntity>> getBeforeList() {
        return ResponseEntity.ok(beforeService.getAllBefore());
    }

    // ==========================================================
    // 4) after 리스트 조회 (관리 / 디버그용)
    // ==========================================================
    @GetMapping("/after")
    public ResponseEntity<List<SheetMusicAfterEntity>> getAfterList() {
        return ResponseEntity.ok(afterService.getAllAfter());
    }

    // ==========================================================
    // 5-1) 키 변경
    // ==========================================================
    @PostMapping("/change-key")
    public ResponseEntity<?> changeKey(
            @RequestParam Long afterId,
            @RequestParam int keyOffset
    ) {
        try {
            String json = afterService.updateKey(afterId, keyOffset);
            return ResponseEntity.ok(json);
        } catch (Exception e) {
            log.error("키 변경 오류", e);
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // ==========================================================
    // 5-2) 성별 키 변환
    // ==========================================================
    @PostMapping("/change-gender")
    public ResponseEntity<?> changeGender(
            @RequestParam Long afterId,
            @RequestParam("genderMode") String genderMode
    ) {
        try {
            String json = afterService.updateGender(afterId, genderMode);
            return ResponseEntity.ok(json);
        } catch (Exception e) {
            log.error("성별 변환 오류", e);
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // ==========================================================
    // 5-3) 자동 보정
    // ==========================================================
    @PostMapping("/auto-fix")
    public ResponseEntity<?> autoFix(
            @RequestParam Long afterId
    ) {
        try {
            String json = afterService.autoFixPitch(afterId);
            return ResponseEntity.ok(json);
        } catch (Exception e) {
            log.error("자동 보정 오류", e);
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // ==========================================================
    // 5-4) 키 + 성별 + 자동보정 통합
    // ==========================================================
    @PostMapping("/adjust")
    public ResponseEntity<?> adjustAll(
            @RequestParam Long afterId,
            @RequestParam(defaultValue = "0") int key,
            @RequestParam(defaultValue = "none") String gender,
            @RequestParam(defaultValue = "false") boolean autoFix
    ) {
        try {
            String json = afterService.updateAll(afterId, key, gender, autoFix);
            return ResponseEntity.ok(json);
        } catch (Exception e) {
            log.error("통합 조정 오류", e);
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
