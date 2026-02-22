package com.mutune.backend.service.sheetMusic;

import com.mutune.backend.Entity.SheetMusicBeforeEntity;
import com.mutune.backend.Entity.UserEntity;
import com.mutune.backend.Repository.SheetMusicBefoRepository;
import com.mutune.backend.Repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SheetMusicBeforeServiceImpl implements SheetMusicBeforeService {

    private final SheetMusicBefoRepository beforeRepository;
    private final UserRepository userRepository;

    @Override
    public SheetMusicBeforeEntity saveFile(MultipartFile file, Long userId) {
        try {
            Path projectRoot = Paths.get(System.getProperty("user.dir"));
            Path uploadDir = projectRoot.resolve("uploads").resolve("input");
            Files.createDirectories(uploadDir);

            String originalName = file.getOriginalFilename();
            String ext = ".pdf";

            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }

            String uuid = UUID.randomUUID().toString().replace("-", "");
            String savedName = uuid + ext;

            Path savePath = uploadDir.resolve(savedName);
            file.transferTo(savePath.toFile());

            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다. userId=" + userId));

            return beforeRepository.save(
                    SheetMusicBeforeEntity.builder()
                            .filename(savedName)
                            .originalName(originalName)
                            .filetype(file.getContentType())
                            .filepath(savePath.toString())
                            .createdAt(LocalDateTime.now())
                            .user(user)
                            .build()
            );

        } catch (Exception e) {
            log.error("파일 저장 실패", e);
            throw new RuntimeException("파일 업로드 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SheetMusicBeforeEntity> getAllBefore() {
        return beforeRepository.findAll();
    }
}
