package com.mutune.backend.service.audiveris;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class AudiverisService {

    /**
     * Audiveris 설치 루트
     * 예: D:/Mutune/backend/Audiveris-CLI
     */
    @Value("${audiveris.home}")
    private String audiverisHome;

    /**
     * ============================================================
     * Audiveris 실행
     * - inputPdf : omr.py가 만든 Preprocessed_Score.pdf
     * - outputDir: Audiveris 결과물 저장 폴더
     * ============================================================
     */
    public void runAudiveris(Path inputPdf, Path outputDir) {

        try {
            if (!Files.exists(inputPdf)) {
                throw new RuntimeException("Audiveris 입력 PDF 없음: " + inputPdf);
            }

            Files.createDirectories(outputDir);

            String audiverisBat =
                    Path.of(audiverisHome, "bin", "audiveris.bat").toString();

            String[] command = {
                    audiverisBat,
                    "-batch",
                    "-export",
                    "-force",
                    "-output", outputDir.toString(),
                    inputPdf.toString()
            };

            log.info("▶ Audiveris 실행");
            log.info("CMD = {}", String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = br.readLine()) != null) {
                    log.info("[AUDIVERIS] {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Audiveris 실패 (exitCode=" + exitCode + ")");
            }

            log.info("✔ Audiveris 변환 완료");

        } catch (Exception e) {
            log.error("Audiveris 실행 오류", e);
            throw new RuntimeException("Audiveris 실행 오류", e);
        }
    }

    /**
     * ============================================================
     * Audiveris 결과 폴더에서 XML 찾기
     * ============================================================
     */
    public Path findXml(Path audiverisOutputDir) {

        try {
            if (!Files.exists(audiverisOutputDir)) {
                throw new RuntimeException("Audiveris 결과 폴더 없음: " + audiverisOutputDir);
            }

            // 1) sheet#1/sheet#1.xml (최우선)
            Path sheet1 = audiverisOutputDir
                    .resolve("sheet#1")
                    .resolve("sheet#1.xml");
            if (Files.exists(sheet1)) {
                return sheet1;
            }

            // 2) score.xml
            Path score = audiverisOutputDir.resolve("score.xml");
            if (Files.exists(score)) {
                return score;
            }

            // 3) 기타 xml (book.xml 제외)
            try (Stream<Path> stream = Files.walk(audiverisOutputDir)) {
                Optional<Path> xml = stream
                        .filter(p -> p.toString().endsWith(".xml"))
                        .filter(p -> !p.getFileName().toString().equalsIgnoreCase("book.xml"))
                        .findFirst();
                if (xml.isPresent()) {
                    return xml.get();
                }
            }

            // 4) mxl(zip)에서 xml 추출
            Path mxl = findMxl(audiverisOutputDir);
            if (mxl != null) {
                Path extracted = audiverisOutputDir.resolve("score.xml");
                extractXmlFromMxl(mxl, extracted);
                if (Files.exists(extracted)) {
                    return extracted;
                }
            }

            throw new RuntimeException("Audiveris XML 결과를 찾을 수 없음");

        } catch (Exception e) {
            log.error("Audiveris XML 탐색 실패", e);
            throw new RuntimeException("Audiveris XML 탐색 실패", e);
        }
    }

    /* ============================================================
       내부 헬퍼
       ============================================================ */

    private Path findMxl(Path folder) throws Exception {
        try (Stream<Path> stream = Files.walk(folder)) {
            return stream
                    .filter(p -> p.toString().toLowerCase().endsWith(".mxl"))
                    .findFirst()
                    .orElse(null);
        }
    }

    private void extractXmlFromMxl(Path mxl, Path outputXml) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(mxl))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".xml")) {
                    Files.copy(
                            zis,
                            outputXml,
                            StandardCopyOption.REPLACE_EXISTING
                    );
                    break;
                }
            }
        }
    }
}
