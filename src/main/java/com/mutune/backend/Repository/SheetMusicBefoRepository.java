package com.mutune.backend.Repository;

import com.mutune.backend.Entity.SheetMusicBeforeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SheetMusicBefoRepository extends JpaRepository<SheetMusicBeforeEntity, Long> {
    List<SheetMusicBeforeEntity> findByCreatedAtBefore(LocalDateTime cutoff);
}