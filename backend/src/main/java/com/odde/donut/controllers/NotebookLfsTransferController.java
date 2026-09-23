package com.odde.donut.controllers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Standard Git LFS Batch and Basic transfer for notebook-scoped attachment bytes. Transfers store
 * and fetch verified content only; they do not accept Git commits or change notebook history.
 */
@RestController
@RequestMapping("/api/notebooks/{notebook}/lfs")
class NotebookLfsTransferController {
  static final String LFS_JSON = "application/vnd.git-lfs+json";
  private static final MediaType LFS_MEDIA_TYPE = MediaType.parseMediaType(LFS_JSON);

  private final AuthorizationService authorizationService;
  private final NotebookAttachmentContent notebookAttachmentContent;

  NotebookLfsTransferController(
      AuthorizationService authorizationService,
      NotebookAttachmentContent notebookAttachmentContent) {
    this.authorizationService = authorizationService;
    this.notebookAttachmentContent = notebookAttachmentContent;
  }

  @PostMapping(
      value = "/objects/batch",
      consumes = {LFS_JSON, LFS_JSON + ";charset=UTF-8"},
      produces = LFS_JSON)
  ResponseEntity<?> batch(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @RequestBody BatchRequest body,
      HttpServletRequest request) {
    ResponseEntity<LfsError> denial = accessDenial(notebook, body.operation());
    if (denial != null) {
      return denial;
    }
    if (body.objects() == null || body.operation() == null) {
      return lfsError(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid batch request");
    }
    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    List<BatchObjectResponse> objects = new ArrayList<>();
    for (BatchObjectRequest object : body.objects()) {
      objects.add(batchObject(notebook, body.operation(), object, authHeader, request));
    }
    return ResponseEntity.ok()
        .contentType(LFS_MEDIA_TYPE)
        .body(new BatchResponse("basic", objects, "sha256"));
  }

  @PutMapping("/objects/{oid}")
  ResponseEntity<?> upload(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("oid") String oid,
      HttpServletRequest request)
      throws IOException {
    ResponseEntity<LfsError> denial = accessDenial(notebook, "upload");
    if (denial != null) {
      return denial;
    }
    String normalizedOid = normalizeOid(oid);
    if (normalizedOid == null) {
      return lfsError(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid object id");
    }
    long size = request.getContentLengthLong();
    if (size < 0) {
      return lfsError(HttpStatus.BAD_REQUEST, "Content-Length required");
    }
    try (InputStream content = request.getInputStream()) {
      boolean stored =
          notebookAttachmentContent.store(notebook.getId(), normalizedOid, size, content);
      if (!stored) {
        return lfsError(HttpStatus.BAD_REQUEST, "Object content does not match oid and size");
      }
    }
    return ResponseEntity.ok().build();
  }

  @GetMapping("/objects/{oid}")
  ResponseEntity<?> download(
      @PathVariable("notebook") @Schema(type = "integer") Notebook notebook,
      @PathVariable("oid") String oid) {
    ResponseEntity<LfsError> denial = accessDenial(notebook, "download");
    if (denial != null) {
      return denial;
    }
    String normalizedOid = normalizeOid(oid);
    if (normalizedOid == null) {
      return lfsError(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid object id");
    }
    Optional<byte[]> bytes = notebookAttachmentContent.get(notebook.getId(), normalizedOid);
    if (bytes.isEmpty()) {
      return lfsError(HttpStatus.NOT_FOUND, "Object does not exist");
    }
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(bytes.get().length)
        .body(bytes.get());
  }

  private BatchObjectResponse batchObject(
      Notebook notebook,
      String operation,
      BatchObjectRequest object,
      String authHeader,
      HttpServletRequest request) {
    String oid = normalizeOid(object.oid());
    long size = object.size() == null ? -1 : object.size();
    if (oid == null || size < 0) {
      return BatchObjectResponse.error(object.oid(), size < 0 ? 0 : size, 422, "Validation error");
    }
    boolean present = notebookAttachmentContent.get(notebook.getId(), oid).isPresent();
    if ("download".equals(operation)) {
      if (!present) {
        return BatchObjectResponse.error(oid, size, 404, "Object does not exist");
      }
      return BatchObjectResponse.withAction(
          oid, size, "download", objectHref(notebook, oid, request), authHeader);
    }
    if (present) {
      return BatchObjectResponse.existing(oid, size);
    }
    return BatchObjectResponse.withAction(
        oid, size, "upload", objectHref(notebook, oid, request), authHeader);
  }

  private ResponseEntity<LfsError> accessDenial(Notebook notebook, String operation) {
    User user = authorizationService.getCurrentUser();
    if (user == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .header("LFS-Authenticate", "Basic realm=\"Donut Git LFS\"")
          .contentType(LFS_MEDIA_TYPE)
          .body(new LfsError("Credentials needed"));
    }
    if ("upload".equals(operation)) {
      if (!user.owns(notebook)) {
        return lfsError(HttpStatus.FORBIDDEN, "Write access denied");
      }
      return null;
    }
    if (!authorizationService.userMayReadNotebook(user, notebook)) {
      return lfsError(HttpStatus.FORBIDDEN, "Read access denied");
    }
    return null;
  }

  private static ResponseEntity<LfsError> lfsError(HttpStatus status, String message) {
    return ResponseEntity.status(status).contentType(LFS_MEDIA_TYPE).body(new LfsError(message));
  }

  private static String objectHref(Notebook notebook, String oid, HttpServletRequest request) {
    return ServletUriComponentsBuilder.fromRequest(request)
        .replacePath("/api/notebooks/{notebook}/lfs/objects/{oid}")
        .buildAndExpand(notebook.getId(), oid)
        .toUriString();
  }

  private static String normalizeOid(String oid) {
    if (oid == null || !oid.matches("(?i)[a-f0-9]{64}")) {
      return null;
    }
    return oid.toLowerCase(Locale.ROOT);
  }

  record BatchRequest(String operation, List<String> transfers, List<BatchObjectRequest> objects) {}

  record BatchObjectRequest(String oid, Long size) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record BatchResponse(
      String transfer,
      List<BatchObjectResponse> objects,
      @JsonProperty("hash_algo") String hashAlgo) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record BatchObjectResponse(
      String oid,
      long size,
      Boolean authenticated,
      Map<String, BatchAction> actions,
      BatchObjectError error) {

    static BatchObjectResponse existing(String oid, long size) {
      return new BatchObjectResponse(oid, size, true, null, null);
    }

    static BatchObjectResponse withAction(
        String oid, long size, String actionName, String href, String authHeader) {
      Map<String, String> headers = new LinkedHashMap<>();
      if (authHeader != null && !authHeader.isBlank()) {
        headers.put(HttpHeaders.AUTHORIZATION, authHeader);
      }
      Map<String, BatchAction> actions = Map.of(actionName, new BatchAction(href, headers));
      return new BatchObjectResponse(oid, size, true, actions, null);
    }

    static BatchObjectResponse error(String oid, long size, int code, String message) {
      return new BatchObjectResponse(oid, size, null, null, new BatchObjectError(code, message));
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record BatchAction(String href, Map<String, String> header) {}

  record BatchObjectError(int code, String message) {}

  record LfsError(String message) {}
}
