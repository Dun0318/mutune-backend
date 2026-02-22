package com.mutune.backend.Controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
public class FileController {

    /**
     * 1) MusicXML 반환
     */
    @GetMapping("/xml/{folder}/{filename}")
    public ResponseEntity<Resource> getXml(
            @PathVariable String folder,
            @PathVariable String filename
    ) {
        try {
            Path path = Paths.get("D:/Mutune/backend/uploads/midi/" + folder + "/" + filename);
            UrlResource resource = new UrlResource(path.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/vnd.recordare.musicxml+xml")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 2) omrA / 전처리 이미지 반환
     *    /api/files/preprocessed/{filename}
     */
    @GetMapping("/preprocessed/{filename}")
    public ResponseEntity<Resource> getPreprocessed(@PathVariable String filename) {

        try {
            Path path = Paths.get("D:/Mutune/backend/uploads/preprocessed/" + filename);
            UrlResource resource = new UrlResource(path.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "image/png")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 3) overlay 이미지 반환 (A안)
     *    /api/files/overlay/{filename}
     */
    @GetMapping("/overlay/{filename}")
    public ResponseEntity<Resource> getOverlay(@PathVariable String filename) {

        try {
            Path path = Paths.get("D:/Mutune/backend/uploads/overlay/" + filename);
            UrlResource resource = new UrlResource(path.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "image/png")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
