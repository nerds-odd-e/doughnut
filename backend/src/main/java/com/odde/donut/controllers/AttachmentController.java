package com.odde.donut.controllers;

import com.odde.donut.entities.Image;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/attachments")
public class AttachmentController {
  private final AuthorizationService authorizationService;

  public AttachmentController(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  @GetMapping("/images/{image}/{fileName}")
  public ResponseEntity<byte[]> showImage(
      @PathVariable("image") @Schema(type = "integer") Image image,
      @PathVariable("fileName") String filename)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertReadAuthorization(image.getNote());
    return image.getResponseEntity("inline");
  }
}
