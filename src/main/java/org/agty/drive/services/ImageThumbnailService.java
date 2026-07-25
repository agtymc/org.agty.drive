package org.agty.drive.services;

import org.agty.drive.dto.FileItemDto;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ImageThumbnailService {

    private static final int THUMBNAIL_SIZE = 160;

    private final FileContentStorageService fileContentStorageService;
    private final FileRepositoryAdapter fileRepositoryAdapter;

    public ImageThumbnailService(FileContentStorageService fileContentStorageService,
                                 FileRepositoryAdapter fileRepositoryAdapter) {
        this.fileContentStorageService = fileContentStorageService;
        this.fileRepositoryAdapter = fileRepositoryAdapter;
    }

    public boolean generateForFile(FileItemDto fileItemDto, byte[] content) {
        if (fileItemDto == null || !fileItemDto.isImagePreview() || fileItemDto.getStorageName() == null || content == null) {
            return false;
        }

        try {
            BufferedImage sourceImage = ImageIO.read(new ByteArrayInputStream(content));
            if (sourceImage == null) {
                return false;
            }

            BufferedImage thumbnail = buildThumbnail(sourceImage);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "png", outputStream);
            fileContentStorageService.save(StoragePathSupport.buildThumbnailStorageName(fileItemDto.getStorageName()), outputStream.toByteArray());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public byte[] readThumbnail(String storageName) {
        if (storageName == null || storageName.isBlank()) {
            return null;
        }
        return fileContentStorageService.read(StoragePathSupport.buildThumbnailStorageName(storageName));
    }

    public void deleteThumbnail(String storageName) {
        if (storageName == null || storageName.isBlank()) {
            return;
        }
        fileContentStorageService.delete(StoragePathSupport.buildThumbnailStorageName(storageName));
    }

    public void ensureThumbnailsForExistingImages() {
        List<FileItemDto> imageFiles = fileRepositoryAdapter.findAllActiveImageFiles();
        for (FileItemDto file : imageFiles) {
            byte[] thumbnail = readThumbnail(file.getStorageName());
            if (thumbnail != null && thumbnail.length > 0) {
                if (!"READY".equalsIgnoreCase(file.getPreviewStatus())) {
                    file.setPreviewStatus("READY");
                    fileRepositoryAdapter.save(file);
                }
                continue;
            }

            byte[] content = fileContentStorageService.read(file.getStorageName());
            boolean generated = generateForFile(file, content);
            file.setPreviewStatus(generated ? "READY" : "FAILED");
            fileRepositoryAdapter.save(file);
        }
    }

    private BufferedImage buildThumbnail(BufferedImage sourceImage) {
        int sourceWidth = sourceImage.getWidth();
        int sourceHeight = sourceImage.getHeight();
        int targetWidth = sourceWidth;
        int targetHeight = sourceHeight;

        if (sourceWidth > THUMBNAIL_SIZE || sourceHeight > THUMBNAIL_SIZE) {
            double scale = Math.min((double) THUMBNAIL_SIZE / sourceWidth, (double) THUMBNAIL_SIZE / sourceHeight);
            targetWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
            targetHeight = Math.max(1, (int) Math.round(sourceHeight * scale));
        }

        BufferedImage targetImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = targetImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return targetImage;
    }

    @Service
    public static class FileRepositoryAdapter {
        private final org.agty.drive.repository.FileRepository fileRepository;

        public FileRepositoryAdapter(org.agty.drive.repository.FileRepository fileRepository) {
            this.fileRepository = fileRepository;
        }

        public List<FileItemDto> findAllActiveImageFiles() {
            return fileRepository.findAllActiveImageFiles();
        }

        public FileItemDto save(FileItemDto fileItemDto) {
            return fileRepository.save(fileItemDto);
        }
    }
}
