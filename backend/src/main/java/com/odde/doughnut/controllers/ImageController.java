package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Image;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/attachments/images")
public class ImageController {

  public ImageController() {}

  @GetMapping("/{image}/{fileName}")
  public ResponseEntity<byte[]> show(
      @PathVariable("image") @Schema(type = "integer") Image image,
      @PathVariable("fileName") String filename) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + image.getName() + "\"")
        .header(HttpHeaders.CONTENT_TYPE, image.getType())
        .body(image.getImageBlob().getData());
  }
}
