package com.mutune.backend.service.sheetMusic;

import com.mutune.backend.DTO.SheetMusicAfterResponseDTO;
import com.mutune.backend.Entity.SheetMusicAfterEntity;

import java.util.List;

/**
 * ============================================================
 * MuTune — After(변환된 악보) 서비스 인터페이스 (최종 확정판)
 * ============================================================
 *
 * 핵심 원칙
 * 1. PDF는 절대 수정하지 않는다 (원본 보존)
 * 2. 모든 편집은 MusicXML 기반으로 수행한다
 * 3. Step2에서 "완성된 악보 XML"을 만든다
 *    (가사 + 코드 + 음표 + 마디 구조 100% 일치)
 * 4. Step3 이후 모든 기능은 이 XML만을 기준으로 동작한다
 * 5. 프론트에는 MusicXML → JSON 변환 결과만 전달한다
 */
public interface SheetMusicAfterService {

    /**
     * ----------------------------------------------------------
     * 1) 최초 변환 (Step2)
     *
     * 전체 파이프라인:
     * - PreprocessService (입력 전처리 + 텍스트 추출)
     * - AudiverisService (OMR → score.xml)
     * - note_sequence.py (음표 시퀀스 생성)
     * - xml_merge_lyrics_chords.py (가사/코드/음표 병합)
     *
     * 결과:
     * - merged_score.xml 생성
     * - SheetMusicAfterEntity 저장
     * - Step3 진입에 필요한 메타데이터 반환
     * ----------------------------------------------------------
     */
    SheetMusicAfterResponseDTO convertSheet(Long beforeId, Long userId);

    /**
     * ----------------------------------------------------------
     * 2) After 전체 리스트 조회 (관리용)
     * ----------------------------------------------------------
     */
    List<SheetMusicAfterEntity> getAllAfter();

    /**
     * ----------------------------------------------------------
     * 3) 키(Key) 이동
     *
     * 기준:
     * - merged_score.xml
     *
     * 처리:
     * - xml_transpose_key.py
     * - xml_autofix_pitch_safety.py 자동 포함
     *
     * 결과:
     * - 변환된 MusicXML → JSON 반환
     * ----------------------------------------------------------
     */
    String updateKey(Long afterId, int keyOffset);

    /**
     * ----------------------------------------------------------
     * 4) 성별 전환 (남/여 키)
     *
     * 기준:
     * - merged_score.xml
     *
     * 처리:
     * - xml_transpose_gender.py
     * - 자동 보정 포함
     *
     * 결과:
     * - MusicXML → JSON 반환
     * ----------------------------------------------------------
     */
    String updateGender(Long afterId, String genderMode);

    /**
     * ----------------------------------------------------------
     * 5) 자동 피치 안정화
     *
     * 사용 시점:
     * - Step3 최초 진입 시
     *
     * 처리:
     * - xml_autofix_pitch_safety.py
     *
     * 결과:
     * - MusicXML → JSON 반환
     * ----------------------------------------------------------
     */
    String autoFixPitch(Long afterId);

    /**
     * ----------------------------------------------------------
     * 6) 통합 변환
     *
     * 실행 순서:
     * - 성별 전환
     * - 키 이동
     * - 자동 피치 보정
     *
     * 기준:
     * - 항상 merged_score.xml
     *
     * 결과:
     * - 최종 MusicXML → JSON 반환
     * ----------------------------------------------------------
     */
    String updateAll(Long afterId, Integer keyOffset, String genderMode, boolean autoFix);

    /**
     * ----------------------------------------------------------
     * 7) After 결과 단건 조회
     *
     * 용도:
     * - Step3 최초 진입
     * - 새로고침 / 재진입
     * ----------------------------------------------------------
     */
    SheetMusicAfterResponseDTO getResult(Long afterId);
}
