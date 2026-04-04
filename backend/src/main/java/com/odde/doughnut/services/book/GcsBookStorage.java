package com.odde.doughnut.services.book;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import java.util.Optional;
import java.util.UUID;

public class GcsBookStorage implements BookStorage {

  private final Storage storage;
  private final String bucket;
  private final String objectPrefix;

  public GcsBookStorage(Storage storage, String bucket, String objectPrefix) {
    this.storage = storage;
    this.bucket = bucket;
    this.objectPrefix = normalizePrefix(objectPrefix);
  }

  @Override
  public String put(byte[] data) {
    String name = objectPrefix + UUID.randomUUID() + ".pdf";
    BlobId blobId = BlobId.of(bucket, name);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/pdf").build();
    storage.create(blobInfo, data);
    return name;
  }

  @Override
  public Optional<byte[]> get(String ref) {
    if (!isAllowedRef(ref)) {
      return Optional.empty();
    }
    Blob blob = storage.get(BlobId.of(bucket, ref.trim()));
    if (blob == null) {
      return Optional.empty();
    }
    return Optional.of(blob.getContent());
  }

  private boolean isAllowedRef(String ref) {
    if (ref == null) {
      return false;
    }
    String r = ref.trim();
    if (r.isEmpty()) {
      return false;
    }
    if (r.startsWith("/") || r.contains("..") || r.contains("\\")) {
      return false;
    }
    if (!objectPrefix.isEmpty() && !r.startsWith(objectPrefix)) {
      return false;
    }
    return true;
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
