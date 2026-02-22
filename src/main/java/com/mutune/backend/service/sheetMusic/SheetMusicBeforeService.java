package com.mutune.backend.service.sheetMusic;

import com.mutune.backend.Entity.SheetMusicBeforeEntity;
import org.springframework.web.multipart.MultipartFile;

public interface SheetMusicBeforeService {

    SheetMusicBeforeEntity saveFile(MultipartFile file, Long userId);

    // 리스트 조회도 포함
    java.util.List<SheetMusicBeforeEntity> getAllBefore();
}
