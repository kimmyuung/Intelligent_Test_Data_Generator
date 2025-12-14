package com.itdg.analyzer.service;

import com.itdg.analyzer.exception.AnalysisFailedException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class SourceHelperService {

    private static final String TEMP_DIR_PREFIX = "itdg-analysis-";

    public File cloneRepository(String gitUrl) {
        try {
            Path tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX + UUID.randomUUID());
            log.info("Cloning repository {} to {}", gitUrl, tempDir);

            Git.cloneRepository()
                    .setURI(gitUrl)
                    .setDirectory(tempDir.toFile())
                    .call();

            return tempDir.toFile();
        } catch (Exception e) {
            log.error("Failed to clone repository: {}", gitUrl, e);
            throw new AnalysisFailedException("Git 리포지토리 복제 실패: " + e.getMessage(), e);
        }
    }

    public File extractZipFile(MultipartFile file) {
        try {
            Path tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX + UUID.randomUUID());
            log.info("Extracting upload file to {}", tempDir);

            try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory())
                        continue;

                    Path targetPath = tempDir.resolve(entry.getName());
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(zis, targetPath);
                    zis.closeEntry();
                }
            }
            return tempDir.toFile();
        } catch (IOException e) {
            log.error("Failed to extract zip file", e);
            throw new AnalysisFailedException("압축 파일 해제 실패: " + e.getMessage(), e);
        }
    }

    public void cleanup(File directory) {
        if (directory != null && directory.exists()) {
            try {
                deleteDirectory(directory);
                log.info("Cleaned up temp directory: {}", directory);
            } catch (IOException e) {
                log.warn("Failed to cleanup temp directory: {}", directory, e);
            }
        }
    }

    private void deleteDirectory(File file) throws IOException {
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            if (entries != null) {
                for (File entry : entries) {
                    deleteDirectory(entry);
                }
            }
        }
        if (!file.delete()) {
            throw new IOException("Failed to delete " + file);
        }
    }
}
