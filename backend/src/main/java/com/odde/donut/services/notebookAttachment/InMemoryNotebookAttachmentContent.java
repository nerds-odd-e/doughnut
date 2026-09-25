package com.odde.donut.services.notebookAttachment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** In-process verified content for non-production automated runs. */
public class InMemoryNotebookAttachmentContent implements NotebookAttachmentContent {

  private final ConcurrentHashMap<String, byte[]> verified = new ConcurrentHashMap<>();
  private final AtomicLong getCalls = new AtomicLong();
  private final AtomicLong storeCalls = new AtomicLong();

  @Override
  public boolean store(Integer notebookId, String sha256Hex, long size, InputStream content)
      throws IOException {
    storeCalls.incrementAndGet();
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
    getCalls.incrementAndGet();
    return Optional.ofNullable(
        verified.get(NotebookAttachmentContentKey.objectKey(notebookId, sha256Hex)));
  }

  /** Access counts for scoping content-store observations to a controller call under test. */
  public long getCalls() {
    return getCalls.get();
  }

  public long storeCalls() {
    return storeCalls.get();
  }

  public void resetAccessCounts() {
    getCalls.set(0);
    storeCalls.set(0);
  }

  /** Forgets all stored content, as when the database behind it is reset. */
  public void clear() {
    verified.clear();
  }
}
