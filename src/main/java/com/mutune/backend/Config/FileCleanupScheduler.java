package com.mutune.backend.Config;

import com.mutune.backend.Entity.SheetMusicAfterEntity;
import com.mutune.backend.Entity.SheetMusicBeforeEntity;
import com.mutune.backend.Repository.SheetMusicAfterRepository;
import com.mutune.backend.Repository.SheetMusicBefoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupScheduler {

    private final SheetMusicBefoRepository beforeRepository;
    private final SheetMusicAfterRepository afterRepository;

    // 1시간마다 실행 (크론: 매 시간 5분 0초)
    @Scheduled(cron = "0 5 * * * *")
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(48);
        log.info("[FileCleanup] start, cutoff={}", cutoff);

        // 변환본 먼저 제거 (FK로 before를 잡고 있을 수 있음)
        List<SheetMusicAfterEntity> afterList = afterRepository.findByCreatedAtBefore(cutoff);
        for (SheetMusicAfterEntity a : afterList) {
            // DB에 저장된 경로는 web 경로(e.g. files/converted/xxx), 실제 파일 경로로 변환
            deletePhysicalIfExists(a.getConvertedFilePath(), "converted");
            afterRepository.deleteById(a.getId());
        }
        log.info("[FileCleanup] deleted AFTER count={}", afterList.size());

        // 원본 제거
        List<SheetMusicBeforeEntity> beforeList = beforeRepository.findByCreatedAtBefore(cutoff);
        for (SheetMusicBeforeEntity b : beforeList) {
            deletePhysicalIfExists(b.getFilepath(), "uploads");
            beforeRepository.deleteById(b.getId());
        }
        log.info("[FileCleanup] deleted BEFORE count={}", beforeList.size());

        log.info("[FileCleanup] done");
    }

    private void deletePhysicalIfExists(String storedPath, String type) {
        try {
            String baseDir = System.getProperty("user.dir");
            String realPath;

            // DB에 무엇을 저장했는지에 따라 처리:
            // 1) web 경로 (files/converted/xxx) → 실제 경로로 변환
            // 2) 절대 경로 (D:\Mutune\backend\converted\xxx) 그대로 사용
            if (storedPath == null) return;

            if (storedPath.startsWith("files/")) {
                // files/uploads/** 또는 files/converted/**
                String sub = storedPath.replace("files/uploads/", "uploads/")
                        .replace("files/converted/", "converted/");
                realPath = baseDir + File.separator + sub.replace("/", File.separator);
            } else {
                // 이미 절대 경로로 저장된 경우
                realPath = storedPath;
            }

            File f = new File(realPath);
            if (f.exists()) {
                if (f.delete()) {
                    log.info("[FileCleanup] deleted file: {}", realPath);
                } else {
                    log.warn("[FileCleanup] failed to delete: {}", realPath);
                }
            }
        } catch (Exception e) {
            log.warn("[FileCleanup] error deleting physical file. path={}, type={}, err={}",
                    storedPath, type, e.toString());
        }
    }
}
