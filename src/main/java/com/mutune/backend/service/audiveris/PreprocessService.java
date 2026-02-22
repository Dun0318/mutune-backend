package com.mutune.backend.service.audiveris;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
//@Service
@RequiredArgsConstructor
public class PreprocessService {

    /* ============================================================
       1. 설정 값 (application-*.yml에서 주입)
       ============================================================ */

    // Python 실행 경로
    @Value("${preprocess.python}")
    private String pythonPath;

    // 1) 입력 전처리 (PDF → 이미지 정규화 + 세션 생성)
    @Value("${preprocess.input.script}")
    private String preprocessInputScript;

    // 2) OMR 전처리 (clean 이미지 생성)
    @Value("${preprocess.omr.script}")
    private String preprocessOmrScript;

    // 전처리 결과 베이스 디렉토리
    // 예: D:/Mutune/backend/uploads/preprocessed_input
    @Value("${preprocess.base-dir}")
    private String preprocessBaseDir;

    // 3) PDF → Text 추출 (가사 / 코드)
    @Value("${preprocess.text_extract.script}")
    private String pdfTextExtractScript;

    // 4) 가사 / 코드 정규화
    @Value("${preprocess.text_normalize.script}")
    private String textScoreNormalizeScript;

    // 5) score.xml → 음표 시퀀스 (Audiveris 이후 단계)
    @Value("${preprocess.note_sequence.script}")
    private String noteSequenceScript;

    // 6) XML 병합 (Audiveris 이후 단계)
    @Value("${preprocess.xml_merge.script}")
    private String xmlMergeScript;

    /**
     * ============================================================
     * 메인 전처리 파이프라인
     *
     * 이 메서드는 “Audiveris 실행 이전 단계”까지만 책임진다.
     *
     * 전체 흐름:
     * 1) preprocess_input.py
     * 2) 세션 디렉토리 탐색
     * 3) preprocess_omr.py
     * 4) OMR 입력 이미지 수집
     * 5) pdf_text_extract.py
     * 6) text_score_normalize.py
     *
     * note_sequence.py / xml_merge_lyrics_chords.py 는
     * AudiverisService에서 score.xml 생성 이후 실행한다.
     * ============================================================
     */
    public PreprocessResult runPreprocess(String inputFilePath) {

        try {
            log.info("Preprocess START");
            log.info("inputFilePath = {}", inputFilePath);

            /* ====================================================
               1) preprocess_input.py 실행
               - 입력 파일을 받아 세션 디렉토리 생성
               - 결과:
                 preprocess_base_dir/{UUID_YYYYMMDD_HHMMSS}/
               ==================================================== */
            runScript(new String[]{
                    pythonPath,
                    preprocessInputScript,
                    inputFilePath
            });

            /* ====================================================
               2) 가장 최근 세션 폴더 탐색
               - metadata.json 존재 여부 기준
               ==================================================== */
            Path sessionPath = findLatestSessionDir();
            log.info("sessionPath 확정 = {}", sessionPath);

            /* ====================================================
               3) preprocess_omr.py 실행
               - stdin으로 sessionPath 전달
               - clean_*.png 생성
               ==================================================== */
            runScript(
                    new String[]{pythonPath, preprocessOmrScript},
                    sessionPath.toString()
            );

            /* ====================================================
               4) OMR 입력 이미지 수집
               - clean_*.png 중 첫 페이지만 사용
               ==================================================== */
            List<String> omrAImagePaths;
            try (Stream<Path> paths = Files.walk(sessionPath)) {
                omrAImagePaths = paths
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().startsWith("clean_"))
                        .filter(p -> p.getFileName().toString().endsWith(".png"))
                        .sorted()
                        .limit(1)
                        .map(p -> p.toAbsolutePath().toString())
                        .collect(Collectors.toList());
            }

            if (omrAImagePaths.isEmpty()) {
                throw new RuntimeException("OMR-A 이미지 생성 실패 (clean_*.png 없음)");
            }

            String firstOmrAPath = omrAImagePaths.get(0);
            log.info("OMR-A 입력 이미지 = {}", firstOmrAPath);

            /* ====================================================
               5) TextPDF 폴더 생성 + PDF 텍스트 추출
               - pdf_text_extract.py
               - 결과: TextPDF/lyrics_chords.json
               ==================================================== */
            Path textPdfDir = sessionPath.resolve("TextPDF");
            Files.createDirectories(textPdfDir);

            // 세션 내부 PDF가 있으면 우선 사용
            Path sessionPdf = sessionPath.resolve("input_1.pdf");
            String textPdfSource =
                    Files.exists(sessionPdf) ? sessionPdf.toString() : inputFilePath;

            runScript(new String[]{
                    pythonPath,
                    pdfTextExtractScript,
                    textPdfSource,
                    sessionPath.toString()
            });

            Path lyricsChordsJson = textPdfDir.resolve("lyrics_chords.json");
            if (!Files.exists(lyricsChordsJson)) {
                throw new RuntimeException("lyrics_chords.json 생성 실패");
            }

            /* ====================================================
               6) 텍스트 정규화
               - text_score_normalize.py
               - 결과: TextPDF/text_score_sequence.json
               ==================================================== */
            Path textScoreSequenceJson =
                    textPdfDir.resolve("text_score_sequence.json");

            runScript(new String[]{
                    pythonPath,
                    textScoreNormalizeScript,
                    lyricsChordsJson.toString(),
                    textScoreSequenceJson.toString()
            });

            if (!Files.exists(textScoreSequenceJson)) {
                throw new RuntimeException("text_score_sequence.json 생성 실패");
            }

            /* ====================================================
               7) 결과 반환
               - Audiveris 이전 단계까지만 포함
               ==================================================== */
            return PreprocessResult.builder()
                    .sessionPath(sessionPath.toString())
                    .omrAImagePaths(omrAImagePaths)
                    .firstOmrAPath(firstOmrAPath)
                    .totalPages(1)
                    .build();

        } catch (Exception e) {
            log.error("Preprocess 실패", e);
            throw new RuntimeException("Preprocess 실패: " + e.getMessage(), e);
        }
    }

    /* ============================================================
       Python 실행 유틸
       ============================================================ */

    private void runScript(String[] command) throws Exception {
        runScript(command, null);
    }

    private void runScript(String[] command, String stdin) throws Exception {

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        if (stdin != null) {
            process.getOutputStream()
                    .write((stdin + "\n").getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().flush();
        }

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
       세션 디렉토리 탐색
       ============================================================ */

    private Path findLatestSessionDir() throws Exception {

        Path baseDir = Path.of(preprocessBaseDir);

        try (Stream<Path> stream = Files.list(baseDir)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(dir -> Files.exists(dir.resolve("metadata.json")))
                    .max(Comparator.comparingLong(this::getLastModified))
                    .orElseThrow(() ->
                            new RuntimeException("preprocess 세션 폴더를 찾을 수 없습니다")
                    );
        }
    }

    private long getLastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception e) {
            return Instant.EPOCH.toEpochMilli();
        }
    }
}
