package com.odde.donut.services.notebookAttachment;

final class NotebookAttachmentContentKey {

  private NotebookAttachmentContentKey() {}

  static String objectKey(Integer notebookId, String sha256Hex) {
    return "notebook/" + notebookId + "/lfs/" + sha256Hex.toLowerCase();
  }
}
