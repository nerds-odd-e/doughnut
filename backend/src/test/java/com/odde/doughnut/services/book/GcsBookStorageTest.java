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
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GcsBookStorageTest {

  @Mock Storage storage;

  @ParameterizedTest
  @CsvSource({
    "pdf, .pdf, application/pdf",
    "epub, .epub, application/epub+zip",
  })
  void put_uploadsWithExpectedExtensionAndContentType(
      String format, String extension, String contentType) {
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

  @ParameterizedTest
  @MethodSource("getBlobPresenceCases")
  void get_returnsBytesOrEmptyWhenBlobPresentOrMissing(
      String prefix, String ref, boolean blobExists) {
    GcsBookStorage cut = new GcsBookStorage(storage, "b", prefix);
    if (blobExists) {
      Blob blob = mock(Blob.class);
      byte[] content = {1, 2, 3};
      when(blob.getContent()).thenReturn(content);
      when(storage.get(BlobId.of("b", ref))).thenReturn(blob);
      assertEquals(Optional.of(content), cut.get(ref));
    } else {
      when(storage.get(BlobId.of("b", ref))).thenReturn(null);
      assertTrue(cut.get(ref).isEmpty());
    }
  }

  static Stream<Arguments> getBlobPresenceCases() {
    return Stream.of(Arguments.of("pre/", "pre/x.pdf", true), Arguments.of("", "a.pdf", false));
  }

  @ParameterizedTest
  @CsvSource({
    "safe/../evil, safe/",
    "/safe/x, safe/",
    "safe\\x, safe/",
    "other/key.pdf, expected/",
  })
  void get_emptyWhenInvalidRefOrWrongPrefix(String ref, String prefix) {
    GcsBookStorage cut = new GcsBookStorage(storage, "b", prefix);
    assertTrue(cut.get(ref).isEmpty());
    verifyNoInteractions(storage);
  }

  @ParameterizedTest
  @CsvSource({
    "pre/, pre/obj.pdf, true",
    "safe/, safe/../evil, false",
  })
  void delete_respectsRefPrefix(String prefix, String ref, boolean callsStorage) {
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
