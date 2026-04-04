package com.odde.doughnut.services.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GcsBookStorageTest {

  @Test
  void put_uploadsWithBucketPrefixAndPdfContentType() {
    Storage storage = mock(Storage.class);
    GcsBookStorage cut = new GcsBookStorage(storage, "my-bucket", "pre");

    byte[] data = "pdf".getBytes(StandardCharsets.UTF_8);
    String ref = cut.put(data);

    assertTrue(ref.startsWith("pre/"));
    assertTrue(ref.endsWith(".pdf"));

    ArgumentCaptor<BlobInfo> infoCaptor = ArgumentCaptor.forClass(BlobInfo.class);
    verify(storage).create(infoCaptor.capture(), eq(data));
    BlobInfo info = infoCaptor.getValue();
    assertEquals("my-bucket", info.getBlobId().getBucket());
    assertEquals(ref, info.getBlobId().getName());
    assertEquals("application/pdf", info.getContentType());
  }

  @Test
  void get_returnsBytesWhenBlobExists() {
    Storage storage = mock(Storage.class);
    Blob blob = mock(Blob.class);
    byte[] content = {1, 2, 3};
    when(blob.getContent()).thenReturn(content);
    when(storage.get(BlobId.of("b", "pre/x.pdf"))).thenReturn(blob);

    GcsBookStorage cut = new GcsBookStorage(storage, "b", "pre/");
    assertEquals(Optional.of(content), cut.get("pre/x.pdf"));
  }

  @Test
  void get_emptyWhenInvalidRef() {
    Storage storage = mock(Storage.class);
    GcsBookStorage cut = new GcsBookStorage(storage, "b", "safe/");
    assertTrue(cut.get("safe/../evil").isEmpty());
    assertTrue(cut.get("/safe/x").isEmpty());
    assertTrue(cut.get("safe\\x").isEmpty());
    verifyNoInteractions(storage);
  }

  @Test
  void get_emptyWhenWrongPrefix() {
    Storage storage = mock(Storage.class);
    GcsBookStorage cut = new GcsBookStorage(storage, "b", "expected/");
    assertTrue(cut.get("other/key.pdf").isEmpty());
    verifyNoInteractions(storage);
  }

  @Test
  void get_emptyWhenNoBlob() {
    Storage storage = mock(Storage.class);
    when(storage.get(BlobId.of("b", "a.pdf"))).thenReturn(null);
    GcsBookStorage cut = new GcsBookStorage(storage, "b", "");
    assertTrue(cut.get("a.pdf").isEmpty());
  }
}
