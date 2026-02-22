package com.mutune.backend.service.audiveris;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PreprocessResult {

    /**
     * 전체 세션 폴더 경로
     * 예: D:/Mutune/backend/uploads/preprocessed_input/xxxx
     */
    private String sessionPath;

    /**
     * OMR-A 이미지 전체 경로 (Audiveris 입력 후보들)
     */
    private List<String> omrAImagePaths;

    /**
     * 첫 번째 OMR-A 이미지
     * → Audiveris에 실제로 넘길 파일
     */
    private String firstOmrAPath;

    /**
     * 프론트에서 사용할 overlay 이미지 목록 (페이지 순서)
     */
    private List<String> overlayImagePaths;

    /**
     * 전체 페이지 수
     */
    private int totalPages;

    private String mergedScoreXmlPath;
}
