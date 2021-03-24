package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.ImageEntity;
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

    @GetMapping("/{imageEntity}/{fileName}")
    public ResponseEntity<byte[]> show(@PathVariable("imageEntity") ImageEntity imageEntity, @PathVariable("fileName") String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + imageEntity.getName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, imageEntity.getType())
                .body(imageBlobRepository.findById(imageEntity.getImageBlobEntityId()).get().getData());
    }
}

