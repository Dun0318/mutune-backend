package com.mutune.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sheet_music_after")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SheetMusicAfterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // before와 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "before_id", nullable = false)
    private SheetMusicBeforeEntity score;

    // 기본 옵션 (프론트에서 조절한 키/템포)
    private int keyChange;
    private int tempoChange;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // 최초 Audiveris 결과 (MIDI)
    private String convertedFileName;
    private String convertedFilePath;

    // 최초 XML
    @Column(name = "quick_xml_path")
    private String quickXmlPath;


    // 1) 키 변경 후 XML
    private String xmlAfterKey;

    // 2) 성별 변환 후 XML (male/female)
    private String xmlAfterGender;

    // 3) 피치 보정 후 XML
    private String xmlAfterPitchFix;

    // 4) 최종 XML (모든 변경 적용 후 확정본)
    private String xmlFinalPath;

    // 5) 최종 PDF (다시 그린 악보)
    private String finalPdfPath;

    // 6) 최종 MIDI (키/성별/피치 반영 후 다시 생성)
    private String finalMidiPath;

    // 7) 성별 변환 모드 (male / female / none)
    private String genderMode;

    // overlay 저장
    @ElementCollection
    @CollectionTable(name = "sheet_music_overlay", joinColumns = @JoinColumn(name = "after_id"))
    @Column(name = "image_path")
    private List<String> overlayImagePaths = new ArrayList<>();
}
