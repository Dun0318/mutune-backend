package com.mutune.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SheetMusicAfterResponseDTO {

    // Step3가 사용하는 afterId
    private Long afterId;

    // Audiveris 결과물 저장 폴더명 (예: abc123_mutune.mid)
    private String convertedFileName;

    // Audiveris 결과물 폴더 경로 (서버 경로)
    private String convertedFilePath;

    // 최초 생성 시 누적된 키/템포 변화값
    private int keyChange;
    private int tempoChange;

    private LocalDateTime createdAt;

    // Step3 → OSMD 로딩용 XML URL (http://localhost:8080/uploads/.../score.xml)
    private String quickXmlPath;

    // PDF 2페이지 이상일 경우 overlay 이미지들
    private List<String> overlayImagePaths;
}
