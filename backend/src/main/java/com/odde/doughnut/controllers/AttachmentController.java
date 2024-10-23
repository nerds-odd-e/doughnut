package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Image;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/attachments")
public class AttachmentController {
  public AttachmentController() {}

  @GetMapping("/images/{image}/{fileName}")
  public ResponseEntity<byte[]> showImage(
      @PathVariable("image") @Schema(type = "integer") Image image,
      @PathVariable("fileName") String filename) {
    return image.getResponseEntity("inline");
  }
}
