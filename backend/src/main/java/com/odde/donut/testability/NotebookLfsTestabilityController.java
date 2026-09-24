package com.odde.donut.testability;

import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.services.notebookAttachment.VerifiedNotebookAttachmentBytes;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.nio.charset.StandardCharsets;
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
class NotebookLfsTestabilityController {

  @Autowired NotebookRepository notebookRepository;
  @Autowired NotebookAttachmentRepository notebookAttachmentRepository;
  @Autowired NotebookAttachmentContent notebookAttachmentContent;
  @Autowired TestabilitySettings testabilitySettings;
  @Autowired NotebookGitCutoverService notebookGitCutoverService;

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
   * Testability-only: stores the tip payload (and optional obsolete payload), projects a root
   * attachment as a pointer, and resets accepted history keeping the notebook's accepted Git
   * metadata so CLI clone can hydrate an already-accepted LFS tip.
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
    if (request.getObsoletePayload() != null) {
      notebookAttachmentContent.storeAsLfsPointer(
          notebook.getId(), request.getObsoletePayload().getBytes(StandardCharsets.UTF_8));
    }
    byte[] payload = request.getPayload().getBytes(StandardCharsets.UTF_8);
    byte[] pointer = notebookAttachmentContent.storeAsLfsPointer(notebook.getId(), payload);

    NotebookAttachment attachment =
        findRootAttachment(notebook.getId(), request.getFilename())
            .orElseGet(NotebookAttachment::new);
    attachment.setNotebook(notebook);
    attachment.setFolder(null);
    attachment.setFilename(request.getFilename());
    attachment.setAcceptedGitContent(pointer);
    notebookAttachmentRepository.save(attachment);

    notebookGitCutoverService.resetHistory(
        notebook, testabilitySettings.getCurrentUTCTimestamp().toInstant());

    AcceptLfsAttachmentTipResponse response = new AcceptLfsAttachmentTipResponse();
    response.setOid(VerifiedNotebookAttachmentBytes.sha256Hex(payload));
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
