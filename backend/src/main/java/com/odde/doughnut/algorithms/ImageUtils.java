package com.odde.doughnut.algorithms;

import org.springframework.core.io.InputStreamSource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageUtils {
    private final int MAX_WIDTH = 600;
    private final int MAX_HEIGHT = 600;
    public ImageUtils() {
    }

    public byte[] toResizedImageByteArray(InputStreamSource file, String originalFilename) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        if (width <= MAX_WIDTH && height <= MAX_HEIGHT) {
            return file.getInputStream().readAllBytes();
        }

        if(width > MAX_WIDTH) {
            height = height * MAX_WIDTH / width;
            width = MAX_WIDTH;
        }

        if(height > MAX_HEIGHT) {
            width = width * MAX_HEIGHT / height;
            height = MAX_HEIGHT;
        }

        BufferedImage resizedImage = resizeImage(originalImage, width, height);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, getFileExtension(originalFilename), baos);
        return baos.toByteArray();
    }

    private String getFileExtension(String name) {
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        return name.substring(lastIndexOf + 1);
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();
        return resizedImage;
    }
}