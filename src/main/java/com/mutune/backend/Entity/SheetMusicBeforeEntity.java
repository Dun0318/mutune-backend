package com.mutune.backend.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sheet_music_before")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SheetMusicBeforeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 업로드 후 서버에 저장된 안전한 파일명
    private String filename;

    // 사용자가 올린 원본 파일명 (한글 포함)
    private String originalName;

    // 파일 MIME 타입
    private String filetype;

    // 실제 저장 절대경로
    private String filepath;

    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    // 사용자 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;
}
