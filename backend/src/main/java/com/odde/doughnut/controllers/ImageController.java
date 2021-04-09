package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Image;
import com.odde.doughnut.entities.repositories.ImageBlobRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/images")
public class ImageController {
    private final ImageBlobRepository imageBlobRepository;

    public ImageController(ImageBlobRepository imageBlobRepository) {
        this.imageBlobRepository = imageBlobRepository;
    }

    @GetMapping("/{image}/{fileName}")
    public ResponseEntity<byte[]> show(@PathVariable("image") Image image, @PathVariable("fileName") String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, image.getType())
                .body(imageBlobRepository.findById(image.getImageBlobId()).get().getData());
    }
}

