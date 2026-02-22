package com.mutune.backend.Repository;

import com.mutune.backend.Entity.SheetMusicAfterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SheetMusicAfterRepository extends JpaRepository<SheetMusicAfterEntity, Long> {

    // 특정 유저의 모든 변환 기록
    List<SheetMusicAfterEntity> findByUserId(Long userId);

    // 원본(before)의 모든 변환 기록
    List<SheetMusicAfterEntity> findByScore_Id(Long beforeId);

    // 일정 시간 이전 기록 조회 (자동삭제 배치용)
    List<SheetMusicAfterEntity> findByCreatedAtBefore(LocalDateTime cutoff);

    // 추가된 기능 1 — 파일명 기반 조회
    Optional<SheetMusicAfterEntity> findByConvertedFileName(String convertedFileName);

    // 추가된 기능 2 — 최종 XML 생성 완료된 항목
    List<SheetMusicAfterEntity> findByXmlFinalPathIsNotNull();
}
