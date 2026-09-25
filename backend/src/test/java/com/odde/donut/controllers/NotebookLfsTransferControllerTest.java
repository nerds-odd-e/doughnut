package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class NotebookLfsTransferControllerTest extends ControllerTestBase {
  private static final MediaType LFS_JSON =
      MediaType.parseMediaType(NotebookLfsTransferController.LFS_JSON);

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired NotebookAttachmentContent notebookAttachmentContent;
  @Autowired NotebookGitBindingRepository notebookGitBindingRepository;

  Notebook notebook;
  String oid;
  byte[] payload;

  @BeforeEach
  void createOwnedNotebookAndPayload() throws Exception {
    currentUser.setUser(makeMe.aUser().please());
    notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    payload = "lfs-transfer-bytes".getBytes(StandardCharsets.UTF_8);
    oid = sha256Hex(payload);
  }

  @Nested
  class UploadAndDownload {
    @Test
    void ownerUploadsAndDownloadsExactBytesThroughBatchAndBasicTransfer() throws Exception {
      String acceptedHeadBefore = acceptedHeadOrNull(notebook);

      MvcResult batchUpload =
          mockMvc
              .perform(
                  post("/api/notebooks/{id}/lfs/objects/batch", notebook.getId())
                      .contentType(LFS_JSON)
                      .accept(LFS_JSON)
                      .content(batchJson("upload", oid, payload.length)))
              .andExpect(status().isOk())
              .andExpect(content().contentTypeCompatibleWith(LFS_JSON))
              .andExpect(jsonPath("$.transfer", equalTo("basic")))
              .andExpect(jsonPath("$.objects[0].oid", equalTo(oid)))
              .andExpect(jsonPath("$.objects[0].actions.upload.href", not(emptyOrNullString())))
              .andReturn();

      JsonNode uploadHref =
          objectMapper
              .readTree(batchUpload.getResponse().getContentAsString())
              .at("/objects/0/actions/upload/href");
      assertThat(
          uploadHref.asText(),
          containsString("/api/notebooks/" + notebook.getId() + "/lfs/objects/"));

      mockMvc
          .perform(
              put("/api/notebooks/{id}/lfs/objects/{oid}", notebook.getId(), oid)
                  .contentType(MediaType.APPLICATION_OCTET_STREAM)
                  .content(payload))
          .andExpect(status().isOk());

      mockMvc
          .perform(
              post("/api/notebooks/{id}/lfs/objects/batch", notebook.getId())
                  .contentType(LFS_JSON)
                  .accept(LFS_JSON)
                  .content(batchJson("upload", oid, payload.length)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.objects[0].actions").doesNotExist());

      mockMvc
          .perform(
              post("/api/notebooks/{id}/lfs/objects/batch", notebook.getId())
                  .contentType(LFS_JSON)
                  .accept(LFS_JSON)
                  .content(batchJson("download", oid, payload.length)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.objects[0].actions.download.href", not(emptyOrNullString())));

      mockMvc
          .perform(get("/api/notebooks/{id}/lfs/objects/{oid}", notebook.getId(), oid))
          .andExpect(status().isOk())
          .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, payload.length))
          .andExpect(content().bytes(payload));

      assertThat(acceptedHeadOrNull(notebook), equalTo(acceptedHeadBefore));
    }

    @Test
    void readerCanDownloadUploadedContent() throws Exception {
      notebookAttachmentContent.store(
          notebook.getId(), oid, payload.length, new ByteArrayInputStream(payload));
      becomeSubscriber(notebook);

      mockMvc
          .perform(get("/api/notebooks/{id}/lfs/objects/{oid}", notebook.getId(), oid))
          .andExpect(status().isOk())
          .andExpect(content().bytes(payload));
    }
  }

  @Nested
  class AuthorizationAndMissingObjects {
    @Test
    void missingAccessReturnsForbiddenLfsError() throws Exception {
      currentUser.setUser(makeMe.aUser().please());

      mockMvc
          .perform(
              post("/api/notebooks/{id}/lfs/objects/batch", notebook.getId())
                  .contentType(LFS_JSON)
                  .accept(LFS_JSON)
                  .content(batchJson("download", oid, payload.length)))
          .andExpect(status().isForbidden())
          .andExpect(content().contentTypeCompatibleWith(LFS_JSON))
          .andExpect(jsonPath("$.message", not(emptyOrNullString())));
    }

    @Test
    void readerCannotUpload() throws Exception {
      becomeSubscriber(notebook);

      mockMvc
          .perform(
              post("/api/notebooks/{id}/lfs/objects/batch", notebook.getId())
                  .contentType(LFS_JSON)
                  .accept(LFS_JSON)
                  .content(batchJson("upload", oid, payload.length)))
          .andExpect(status().isForbidden());
    }

    @Test
    void missingObjectReturnsStandardBatchObjectError() throws Exception {
      mockMvc
          .perform(
              post("/api/notebooks/{id}/lfs/objects/batch", notebook.getId())
                  .contentType(LFS_JSON)
                  .accept(LFS_JSON)
                  .content(batchJson("download", oid, payload.length)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.objects[0].error.code", equalTo(404)))
          .andExpect(jsonPath("$.objects[0].actions").doesNotExist());
    }

    @Test
    void hashAloneDoesNotGrantAccessAcrossNotebooks() throws Exception {
      notebookAttachmentContent.store(
          notebook.getId(), oid, payload.length, new ByteArrayInputStream(payload));
      Notebook other = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();

      mockMvc
          .perform(
              post("/api/notebooks/{id}/lfs/objects/batch", other.getId())
                  .contentType(LFS_JSON)
                  .accept(LFS_JSON)
                  .content(batchJson("download", oid, payload.length)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.objects[0].error.code", equalTo(404)));

      mockMvc
          .perform(get("/api/notebooks/{id}/lfs/objects/{oid}", other.getId(), oid))
          .andExpect(status().isNotFound());
    }
  }

  private void becomeSubscriber(Notebook notebook) {
    User reader = makeMe.aUser().please();
    var subscription = makeMe.aSubscription().forNotebook(notebook).forUser(reader).please();
    reader.getSubscriptions().add(subscription);
    currentUser.setUser(reader);
  }

  private static String batchJson(String operation, String oid, long size) {
    return """
        {
          "operation": "%s",
          "transfers": ["basic"],
          "objects": [{"oid": "%s", "size": %d}]
        }
        """
        .formatted(operation, oid, size);
  }

  private String acceptedHeadOrNull(Notebook nb) {
    return notebookGitBindingRepository
        .findByNotebook_Id(nb.getId())
        .map(NotebookGitBinding::getAcceptedGitObjectId)
        .orElse(null);
  }

  private static String sha256Hex(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }
}
