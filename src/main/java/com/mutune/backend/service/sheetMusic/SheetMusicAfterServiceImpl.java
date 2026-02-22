package com.mutune.backend.service.sheetMusic;

import com.mutune.backend.DTO.SheetMusicAfterResponseDTO;
import com.mutune.backend.Entity.SheetMusicAfterEntity;
import com.mutune.backend.Entity.SheetMusicBeforeEntity;
import com.mutune.backend.Entity.UserEntity;
import com.mutune.backend.Repository.SheetMusicAfterRepository;
import com.mutune.backend.Repository.SheetMusicBefoRepository;
import com.mutune.backend.Repository.UserRepository;
import com.mutune.backend.service.audiveris.AudiverisService;
import com.mutune.backend.service.audiveris.PreprocessResult;
import com.mutune.backend.service.audiveris.PreprocessService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
//@Service
@RequiredArgsConstructor
@Transactional
public class SheetMusicAfterServiceImpl implements SheetMusicAfterService {

    private final SheetMusicBefoRepository beforeRepository;
    private final SheetMusicAfterRepository afterRepository;
    private final UserRepository userRepository;

    private final PreprocessService preprocessService;
    private final AudiverisService audiverisService;

    /* ============================================================
       Python 스크립트 설정
       ============================================================ */
    @Value("${preprocess.python}")
    private String pythonPath;

    @Value("${preprocess.harmony_extract.script}")
    private String harmonyExtractScript;

    @Value("${preprocess.note_sequence.script}")
    private String noteSequenceScript;

    @Value("${preprocess.xml_merge.script}")
    private String xmlMergeScript;

    /* ============================================================
       1. 최초 변환 (Before → After)
       ============================================================ */
    @Override
    public SheetMusicAfterResponseDTO convertSheet(Long beforeId, Long userId) {

        SheetMusicBeforeEntity before = beforeRepository.findById(beforeId)
                .orElseThrow(() -> new RuntimeException("원본 악보 없음"));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        try {
            String inputPath = before.getFilepath();
            log.info("1. SheetMusic 변환 시작");
            log.info("inputPath = {}", inputPath);

            /* =====================================================
               2. Python 전처리 파이프라인
               ===================================================== */
            PreprocessResult preprocessResult =
                    preprocessService.runPreprocess(inputPath);

            Path sessionPath = Path.of(preprocessResult.getSessionPath());
            log.info("sessionPath = {}", sessionPath);

            /* =====================================================
               3. Audiveris 실행
               ===================================================== */
            Path preprocessedPdf = sessionPath.resolve("Preprocessed_Score.pdf");
            Path audiverisOutputDir = sessionPath.resolve("audiveris");

            audiverisService.runAudiveris(preprocessedPdf, audiverisOutputDir);

            Path scoreXml = audiverisService.findXml(audiverisOutputDir);
            log.info("Audiveris XML Path = {}", scoreXml);

            if (scoreXml == null || !Files.exists(scoreXml)) {
                throw new RuntimeException("score.xml 생성 실패");
            }

            /* =====================================================
               4. score.xml → note_sequence.json
               ===================================================== */
            Path noteSequenceJson =
                    sessionPath.resolve("TextPDF").resolve("note_sequence.json");

            runScript(new String[]{
                    pythonPath,
                    noteSequenceScript,
                    scoreXml.toString(),
                    noteSequenceJson.toString()
            });

            if (!Files.exists(noteSequenceJson)) {
                throw new RuntimeException("note_sequence.json 생성 실패");
            }

            /* =====================================================
               5. 기존 텍스트 시퀀스 (가사/코드)
               ===================================================== */
            Path textScoreSequenceJson =
                    sessionPath.resolve("TextPDF").resolve("text_score_sequence.json");

            if (!Files.exists(textScoreSequenceJson)) {
                throw new RuntimeException("text_score_sequence.json 없음");
            }

            /* =====================================================
   5-1. harmony_timed.json 생성
   ===================================================== */


            Path harmonyTimedJson =
                    sessionPath.resolve("TextPDF").resolve("harmony_timed.json");

            runScript(new String[]{
                    pythonPath,
                    harmonyExtractScript,
                    noteSequenceJson.toString(),
                    harmonyTimedJson.toString()
            });

            if (!Files.exists(harmonyTimedJson)) {
                throw new RuntimeException("harmony_timed.json 생성 실패");
            }

            /* =====================================================
               6. XML 병합
               ===================================================== */

            Path mergedXml = audiverisOutputDir.resolve("merged_score.xml");

            runScript(new String[]{
                    pythonPath,
                    xmlMergeScript,
                    scoreXml.toString(),
                    noteSequenceJson.toString(),
                    textScoreSequenceJson.toString(),
                    harmonyTimedJson.toString(),
                    mergedXml.toString()
            });

            if (!Files.exists(mergedXml)) {
                throw new RuntimeException("merged_score.xml 생성 실패");
            }


            /* =====================================================
               7. After 엔티티 저장
               ===================================================== */
            SheetMusicAfterEntity saved = afterRepository.save(
                    SheetMusicAfterEntity.builder()
                            .user(user)
                            .score(before)
                            .convertedFileName("merged_score")
                            .convertedFilePath(sessionPath.toString())
                            .quickXmlPath(mergedXml.toString())
                            .overlayImagePaths(preprocessResult.getOverlayImagePaths())
                            .keyChange(0)
                            .tempoChange(0)
                            .createdAt(LocalDateTime.now())
                            .build()
            );

            /* =====================================================
               8. 응답 반환
               ===================================================== */
            return SheetMusicAfterResponseDTO.builder()
                    .afterId(saved.getId())
                    .convertedFileName(saved.getConvertedFileName())
                    .convertedFilePath(saved.getConvertedFilePath())
                    .quickXmlPath(saved.getQuickXmlPath())
                    .overlayImagePaths(saved.getOverlayImagePaths())
                    .keyChange(saved.getKeyChange())
                    .tempoChange(saved.getTempoChange())
                    .createdAt(saved.getCreatedAt())
                    .build();

        } catch (Exception e) {
            log.error("SheetMusic 변환 실패", e);
            throw new RuntimeException("SheetMusic 변환 실패: " + e.getMessage(), e);
        }
    }

    /* ============================================================
       Python 실행 유틸
       ============================================================ */
    private void runScript(String[] command) throws Exception {

        log.info("Python 실행 명령어:");
        log.info(String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                log.info("[PYTHON] {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Python 스크립트 실패");
        }
    }

    /* ============================================================
       Step3 이후 기능 (미구현)
       ============================================================ */
    @Override
    public String updateKey(Long afterId, int keyOffset) {
        throw new UnsupportedOperationException("updateKey는 Step3 이후 구현");
    }

    @Override
    public String updateGender(Long afterId, String genderMode) {
        throw new UnsupportedOperationException("updateGender는 Step3 이후 구현");
    }

    @Override
    public String autoFixPitch(Long afterId) {
        throw new UnsupportedOperationException("autoFixPitch는 Step3 이후 구현");
    }

    @Override
    public String updateAll(Long afterId, Integer keyOffset, String genderMode, boolean autoFix) {
        throw new UnsupportedOperationException("updateAll은 Step3 이후 구현");
    }

    @Override
    public SheetMusicAfterResponseDTO getResult(Long afterId) {
        SheetMusicAfterEntity after = afterRepository.findById(afterId)
                .orElseThrow(() -> new RuntimeException("afterId 없음"));

        return SheetMusicAfterResponseDTO.builder()
                .afterId(after.getId())
                .convertedFileName(after.getConvertedFileName())
                .convertedFilePath(after.getConvertedFilePath())
                .quickXmlPath(after.getQuickXmlPath())
                .overlayImagePaths(after.getOverlayImagePaths())
                .keyChange(after.getKeyChange())
                .tempoChange(after.getTempoChange())
                .createdAt(after.getCreatedAt())
                .build();
    }

    @Override
    public List<SheetMusicAfterEntity> getAllAfter() {
        return afterRepository.findAll();
    }
}
