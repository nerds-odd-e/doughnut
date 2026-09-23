package com.odde.donut.services.notebookAttachment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-process verified content for non-production automated runs. */
public class InMemoryNotebookAttachmentContent implements NotebookAttachmentContent {

  private final ConcurrentHashMap<String, byte[]> verified = new ConcurrentHashMap<>();

  @Override
  public boolean store(Integer notebookId, String sha256Hex, long size, InputStream content)
      throws IOException {
    String key = NotebookAttachmentContentKey.objectKey(notebookId, sha256Hex);
    if (verified.containsKey(key)) {
      return true;
    }
    byte[] bytes = VerifiedNotebookAttachmentBytes.readMatchingOrNull(content, sha256Hex, size);
    if (bytes == null) {
      return false;
    }
    verified.putIfAbsent(key, bytes);
    return true;
  }

  @Override
  public Optional<byte[]> get(Integer notebookId, String sha256Hex) {
    return Optional.ofNullable(
        verified.get(NotebookAttachmentContentKey.objectKey(notebookId, sha256Hex)));
  }
}
