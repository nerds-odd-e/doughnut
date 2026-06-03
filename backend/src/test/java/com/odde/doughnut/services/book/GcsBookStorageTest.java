package com.odde.doughnut.services.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

class GcsBookStorageTest {

  @ParameterizedTest
  @CsvSource({
    "pdf, .pdf, application/pdf",
    "epub, .epub, application/epub+zip",
  })
  void put_uploadsWithExpectedExtensionAndContentType(
      String format, String extension, String contentType) {
    Storage storage = mock(Storage.class);
    GcsBookStorage cut = new GcsBookStorage(storage, "my-bucket", "pre");

    byte[] data = format.getBytes(StandardCharsets.UTF_8);
    String ref = cut.put(data, format);

    assertTrue(ref.startsWith("pre/"));
    assertTrue(ref.endsWith(extension));

    ArgumentCaptor<BlobInfo> infoCaptor = ArgumentCaptor.forClass(BlobInfo.class);
    verify(storage).create(infoCaptor.capture(), eq(data));
    BlobInfo info = infoCaptor.getValue();
    assertEquals("my-bucket", info.getBlobId().getBucket());
    assertEquals(ref, info.getBlobId().getName());
    assertEquals(contentType, info.getContentType());
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

  @ParameterizedTest
  @CsvSource({
    "safe/../evil",
    "/safe/x",
    "safe\\x",
  })
  void get_emptyWhenInvalidRef(String ref) {
    Storage storage = mock(Storage.class);
    GcsBookStorage cut = new GcsBookStorage(storage, "b", "safe/");
    assertTrue(cut.get(ref).isEmpty());
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

  @ParameterizedTest
  @CsvSource({
    "pre/, pre/obj.pdf, true",
    "safe/, safe/../evil, false",
  })
  void delete_respectsRefPrefix(String prefix, String ref, boolean callsStorage) {
    Storage storage = mock(Storage.class);
    GcsBookStorage cut = new GcsBookStorage(storage, "b", prefix);
    cut.delete(ref);
    if (callsStorage) {
      verify(storage).delete(BlobId.of("b", ref));
      verifyNoMoreInteractions(storage);
    } else {
      verifyNoInteractions(storage);
    }
  }
}
