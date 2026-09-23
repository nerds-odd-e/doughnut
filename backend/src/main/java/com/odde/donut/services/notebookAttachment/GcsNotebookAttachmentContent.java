package com.odde.donut.services.notebookAttachment;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/** Production verified content using the shared Google {@link Storage} client. */
public class GcsNotebookAttachmentContent implements NotebookAttachmentContent {

  private final Storage storage;
  private final String bucket;
  private final String objectPrefix;

  public GcsNotebookAttachmentContent(Storage storage, String bucket, String objectPrefix) {
    this.storage = storage;
    this.bucket = bucket;
    this.objectPrefix = normalizePrefix(objectPrefix);
  }

  @Override
  public boolean store(Integer notebookId, String sha256Hex, long size, InputStream content)
      throws IOException {
    BlobId blobId = blobId(notebookId, sha256Hex);
    if (storage.get(blobId) != null) {
      return true;
    }
    byte[] bytes = VerifiedNotebookAttachmentBytes.readMatchingOrNull(content, sha256Hex, size);
    if (bytes == null) {
      return false;
    }
    try {
      storage.create(
          BlobInfo.newBuilder(blobId).build(), bytes, Storage.BlobTargetOption.doesNotExist());
      return true;
    } catch (StorageException e) {
      if (e.getCode() == 412) {
        return true;
      }
      throw e;
    }
  }

  @Override
  public Optional<byte[]> get(Integer notebookId, String sha256Hex) {
    Blob blob = storage.get(blobId(notebookId, sha256Hex));
    if (blob == null) {
      return Optional.empty();
    }
    return Optional.of(blob.getContent());
  }

  private BlobId blobId(Integer notebookId, String sha256Hex) {
    return BlobId.of(
        bucket, objectPrefix + NotebookAttachmentContentKey.objectKey(notebookId, sha256Hex));
  }

  private static String normalizePrefix(String prefix) {
    if (prefix == null) {
      return "";
    }
    String p = prefix.trim();
    if (p.isEmpty()) {
      return "";
    }
    return p.endsWith("/") ? p : p + "/";
  }
}
