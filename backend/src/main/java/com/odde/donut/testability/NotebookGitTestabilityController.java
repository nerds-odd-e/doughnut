package com.odde.donut.testability;

import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.NotebookGitAttachmentRepresentation;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.services.notebookGit.NotebookGitAttributes;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import com.odde.donut.services.notebookGit.NotebookGitLfsPointer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"e2e", "test"})
@RequestMapping("/api/testability")
class NotebookGitTestabilityController {

  @Autowired NotebookRepository notebookRepository;
  @Autowired NotebookGitBindingRepository notebookGitBindingRepository;
  @Autowired NotebookAttachmentRepository notebookAttachmentRepository;
  @Autowired NotebookAttachmentContent notebookAttachmentContent;
  @Autowired TestabilitySettings testabilitySettings;
  @Autowired NotebookGitCutoverService notebookGitCutoverService;

  @Schema(name = "ResnapshotNotebookGitBindingRequest")
  @Getter
  @Setter
  static class ResnapshotNotebookGitBindingRequest {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String notebookName;
  }

  @Schema(name = "AcceptLfsAttachmentTipRequest")
  @Getter
  @Setter
  static class AcceptLfsAttachmentTipRequest {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String notebookName;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String filename;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String payload;

    /** Optional prior payload retained in the content store only (not the tip). */
    private String obsoletePayload;
  }

  /**
   * Testability-only: simulates content changes landing after a notebook's real cutover by
   * replacing its accepted Git binding with a fresh snapshot of its current content. See {@link
   * NotebookGitCutoverService#resetHistory}.
   */
  @PostMapping("/resnapshot_notebook_git_binding_for_testability")
  @Transactional
  public String resnapshotNotebookGitBindingForTestability(
      @RequestBody ResnapshotNotebookGitBindingRequest request) {
    if (Strings.isEmpty(request.getNotebookName())) {
      throw new IllegalArgumentException("notebookName is required and cannot be empty");
    }
    notebookGitCutoverService.resetHistory(
        requireNotebook(request.getNotebookName()),
        testabilitySettings.getCurrentUTCTimestamp().toInstant());
    return "OK";
  }

  /**
   * Testability-only: demotes a notebook to legacy RAW attachment storage and clears LFS attributes
   * so pull-based fixtures keep proving the raw journey after creation selects LFS.
   */
  @PostMapping("/force_raw_notebook_git_binding_for_testability")
  @Transactional
  public String forceRawNotebookGitBindingForTestability(
      @RequestBody ResnapshotNotebookGitBindingRequest request) {
    if (Strings.isEmpty(request.getNotebookName())) {
      throw new IllegalArgumentException("notebookName is required and cannot be empty");
    }
    Notebook notebook = requireNotebook(request.getNotebookName());
    NotebookGitBinding binding =
        notebookGitBindingRepository
            .findByNotebook_Id(notebook.getId())
            .orElseThrow(() -> new IllegalArgumentException("Notebook has no Git binding"));
    binding.setAttachmentRepresentation(NotebookGitAttachmentRepresentation.RAW);
    notebookGitBindingRepository.save(binding);
    notebookGitCutoverService.resetHistory(
        notebook, testabilitySettings.getCurrentUTCTimestamp().toInstant(), List.of());
    return "OK";
  }

  /**
   * Testability-only: selects LFS representation, stores the tip payload (and optional obsolete
   * payload), projects a root attachment as a pointer, and resets accepted history with LFS
   * attributes so CLI clone can hydrate an already-accepted LFS tip.
   */
  @PostMapping("/accept_lfs_attachment_tip_for_testability")
  @Transactional
  public AcceptLfsAttachmentTipResponse acceptLfsAttachmentTipForTestability(
      @RequestBody AcceptLfsAttachmentTipRequest request) throws Exception {
    if (Strings.isEmpty(request.getNotebookName())
        || Strings.isEmpty(request.getFilename())
        || request.getPayload() == null) {
      throw new IllegalArgumentException("notebookName, filename, and payload are required");
    }
    Notebook notebook = requireNotebook(request.getNotebookName());
    NotebookGitBinding binding =
        notebookGitBindingRepository
            .findByNotebook_Id(notebook.getId())
            .orElseThrow(() -> new IllegalArgumentException("Notebook has no Git binding"));
    binding.setAttachmentRepresentation(NotebookGitAttachmentRepresentation.LFS);
    notebookGitBindingRepository.save(binding);

    if (request.getObsoletePayload() != null) {
      storePayload(notebook.getId(), request.getObsoletePayload().getBytes(StandardCharsets.UTF_8));
    }
    byte[] payload = request.getPayload().getBytes(StandardCharsets.UTF_8);
    String oid = storePayload(notebook.getId(), payload);
    byte[] pointer = NotebookGitLfsPointer.format(oid, payload.length);

    NotebookAttachment attachment =
        findRootAttachment(notebook.getId(), request.getFilename())
            .orElseGet(NotebookAttachment::new);
    attachment.setNotebook(notebook);
    attachment.setFolder(null);
    attachment.setFilename(request.getFilename());
    attachment.setAcceptedGitContent(pointer);
    notebookAttachmentRepository.save(attachment);

    notebookGitCutoverService.resetHistory(
        notebook,
        testabilitySettings.getCurrentUTCTimestamp().toInstant(),
        NotebookGitAttributes.initialMetadata());

    AcceptLfsAttachmentTipResponse response = new AcceptLfsAttachmentTipResponse();
    response.setOid(oid);
    response.setSize(payload.length);
    return response;
  }

  /**
   * Testability-only: returns whether a root attachment row is present, its MySQL-projected
   * accepted Git content when present, and whether the content store holds a given digest. Row
   * absence does not imply content-store deletion; the digest check still runs.
   */
  @PostMapping("/inspect_notebook_lfs_attachment_for_testability")
  @Transactional(readOnly = true)
  public InspectNotebookLfsAttachmentResponse inspectNotebookLfsAttachmentForTestability(
      @RequestBody InspectNotebookLfsAttachmentRequest request) {
    if (Strings.isEmpty(request.getNotebookName())
        || Strings.isEmpty(request.getFilename())
        || Strings.isEmpty(request.getOid())) {
      throw new IllegalArgumentException("notebookName, filename, and oid are required");
    }
    Notebook notebook = requireNotebook(request.getNotebookName());
    InspectNotebookLfsAttachmentResponse response = new InspectNotebookLfsAttachmentResponse();
    findRootAttachment(notebook.getId(), request.getFilename())
        .ifPresentOrElse(
            attachment -> {
              byte[] accepted = attachment.getAcceptedGitContent();
              response.setAttachmentPresent(true);
              response.setAcceptedGitContentLength(accepted.length);
              response.setAcceptedGitContentUtf8(new String(accepted, StandardCharsets.US_ASCII));
            },
            () -> response.setAttachmentPresent(false));
    var stored = notebookAttachmentContent.get(notebook.getId(), request.getOid());
    response.setObjectStored(stored.isPresent());
    stored.ifPresent(bytes -> response.setStoredObjectSize((long) bytes.length));
    return response;
  }

  private Optional<NotebookAttachment> findRootAttachment(Integer notebookId, String filename) {
    return notebookAttachmentRepository.findByNotebook_Id(notebookId).stream()
        .filter(row -> filename.equals(row.getFilename()) && row.getFolder() == null)
        .findFirst();
  }

  private Notebook requireNotebook(String notebookName) {
    return notebookRepository
        .findFirstByNameAndDeletedAtIsNullOrderByIdAsc(new DisplayName(notebookName))
        .orElseThrow(() -> new IllegalArgumentException("No notebook with name: " + notebookName));
  }

  private String storePayload(Integer notebookId, byte[] payload) throws Exception {
    String oid = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
    if (!notebookAttachmentContent.store(
        notebookId, oid, payload.length, new ByteArrayInputStream(payload))) {
      throw new IllegalStateException("Failed to store LFS payload for testability");
    }
    return oid;
  }

  @Schema(name = "AcceptLfsAttachmentTipResponse")
  @Getter
  @Setter
  static class AcceptLfsAttachmentTipResponse {
    private String oid;
    private long size;
  }

  @Schema(name = "InspectNotebookLfsAttachmentRequest")
  @Getter
  @Setter
  static class InspectNotebookLfsAttachmentRequest {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String notebookName;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String filename;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String oid;
  }

  @Schema(name = "InspectNotebookLfsAttachmentResponse")
  @Getter
  @Setter
  static class InspectNotebookLfsAttachmentResponse {
    private boolean attachmentPresent;
    private Integer acceptedGitContentLength;
    private String acceptedGitContentUtf8;
    private boolean objectStored;
    private Long storedObjectSize;
  }
}
