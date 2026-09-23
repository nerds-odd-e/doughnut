package com.odde.donut.services.notebookAttachment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

class NotebookAttachmentContentTest {

  private static final Integer NOTEBOOK_ID = 42;
  private static final byte[] PAYLOAD = "exact-notebook-bytes".getBytes(StandardCharsets.UTF_8);
  private static final String DIGEST = sha256Hex(PAYLOAD);

  @Nested
  @ExtendWith(MockitoExtension.class)
  class GcsStore {

    @Mock Storage storage;

    GcsNotebookAttachmentContent cut;

    @BeforeEach
    void createCut() {
      cut = new GcsNotebookAttachmentContent(storage, "notebook-bucket", "pre");
    }

    @Test
    void store_persistsExactBytesAndGetReturnsThem() throws Exception {
      AtomicReference<byte[]> stored = new AtomicReference<>();
      when(storage.get(blobId()))
          .thenAnswer(
              invocation -> {
                byte[] bytes = stored.get();
                if (bytes == null) {
                  return null;
                }
                Blob blob = mock(Blob.class);
                when(blob.getContent()).thenReturn(bytes);
                return blob;
              });
      when(storage.create(
              any(BlobInfo.class), any(byte[].class), eq(Storage.BlobTargetOption.doesNotExist())))
          .thenAnswer(
              invocation -> {
                stored.set(invocation.getArgument(1));
                return mock(Blob.class);
              });

      assertTrue(cut.store(NOTEBOOK_ID, DIGEST, PAYLOAD.length, new ByteArrayInputStream(PAYLOAD)));

      ArgumentCaptor<BlobInfo> infoCaptor = ArgumentCaptor.forClass(BlobInfo.class);
      verify(storage)
          .create(infoCaptor.capture(), eq(PAYLOAD), eq(Storage.BlobTargetOption.doesNotExist()));
      assertEquals("notebook-bucket", infoCaptor.getValue().getBlobId().getBucket());
      assertEquals("pre/notebook/42/lfs/" + DIGEST, infoCaptor.getValue().getBlobId().getName());
      assertArrayEquals(PAYLOAD, cut.get(NOTEBOOK_ID, DIGEST).orElseThrow());
    }

    @Test
    void store_wrongSizeOrDigestStoresNothingVerified() throws Exception {
      when(storage.get(blobId())).thenReturn(null);
      assertFalse(
          cut.store(NOTEBOOK_ID, DIGEST, PAYLOAD.length + 1, new ByteArrayInputStream(PAYLOAD)));
      verify(storage, never()).create(any(BlobInfo.class), any(byte[].class), any());
      assertEquals(Optional.empty(), cut.get(NOTEBOOK_ID, DIGEST));

      String otherDigest = sha256Hex("other".getBytes(StandardCharsets.UTF_8));
      BlobId otherBlobId = BlobId.of("notebook-bucket", "pre/notebook/42/lfs/" + otherDigest);
      when(storage.get(otherBlobId)).thenReturn(null);
      assertFalse(
          cut.store(NOTEBOOK_ID, otherDigest, PAYLOAD.length, new ByteArrayInputStream(PAYLOAD)));
      verify(storage, never()).create(any(BlobInfo.class), any(byte[].class), any());
      assertEquals(Optional.empty(), cut.get(NOTEBOOK_ID, otherDigest));
    }

    @Test
    void store_failedUploadStoresNothingVerified() throws Exception {
      when(storage.get(blobId())).thenReturn(null);
      when(storage.create(
              any(BlobInfo.class), any(byte[].class), eq(Storage.BlobTargetOption.doesNotExist())))
          .thenThrow(new StorageException(500, "upload failed"));

      assertThrows(
          StorageException.class,
          () -> cut.store(NOTEBOOK_ID, DIGEST, PAYLOAD.length, new ByteArrayInputStream(PAYLOAD)));

      assertEquals(Optional.empty(), cut.get(NOTEBOOK_ID, DIGEST));
    }

    @Test
    void store_sameDigestRetryDoesNotOverwriteValidContent() throws Exception {
      byte[] original = PAYLOAD;
      Blob existing = mock(Blob.class);
      when(existing.getContent()).thenReturn(original);
      when(storage.get(blobId())).thenReturn(existing);

      byte[] differentClaim = "different-bytes-same-claim".getBytes(StandardCharsets.UTF_8);
      assertTrue(
          cut.store(
              NOTEBOOK_ID,
              DIGEST,
              differentClaim.length,
              new ByteArrayInputStream(differentClaim)));

      verify(storage, never()).create(any(BlobInfo.class), any(byte[].class), any());
      assertArrayEquals(original, cut.get(NOTEBOOK_ID, DIGEST).orElseThrow());
    }

    private BlobId blobId() {
      return BlobId.of("notebook-bucket", "pre/notebook/42/lfs/" + DIGEST);
    }
  }

  @Nested
  class InMemoryStore {

    InMemoryNotebookAttachmentContent memory;

    @BeforeEach
    void emptyStore() {
      memory = new InMemoryNotebookAttachmentContent();
    }

    @Test
    void store_persistsExactBytesAndGetReturnsThem() throws Exception {
      assertTrue(
          memory.store(NOTEBOOK_ID, DIGEST, PAYLOAD.length, new ByteArrayInputStream(PAYLOAD)));
      assertArrayEquals(PAYLOAD, memory.get(NOTEBOOK_ID, DIGEST).orElseThrow());
    }

    @Test
    void store_wrongSizeOrDigestStoresNothingVerified() throws Exception {
      assertFalse(
          memory.store(NOTEBOOK_ID, DIGEST, PAYLOAD.length + 1, new ByteArrayInputStream(PAYLOAD)));
      assertEquals(Optional.empty(), memory.get(NOTEBOOK_ID, DIGEST));

      String otherDigest = sha256Hex("other".getBytes(StandardCharsets.UTF_8));
      assertFalse(
          memory.store(
              NOTEBOOK_ID, otherDigest, PAYLOAD.length, new ByteArrayInputStream(PAYLOAD)));
      assertEquals(Optional.empty(), memory.get(NOTEBOOK_ID, otherDigest));
    }

    @Test
    void store_failedStreamStoresNothingVerified() {
      InputStream failing =
          new InputStream() {
            @Override
            public int read() throws IOException {
              throw new IOException("interrupted");
            }
          };
      assertThrows(
          IOException.class, () -> memory.store(NOTEBOOK_ID, DIGEST, PAYLOAD.length, failing));
      assertEquals(Optional.empty(), memory.get(NOTEBOOK_ID, DIGEST));
    }

    @Test
    void store_sameDigestRetryDoesNotOverwriteValidContent() throws Exception {
      assertTrue(
          memory.store(NOTEBOOK_ID, DIGEST, PAYLOAD.length, new ByteArrayInputStream(PAYLOAD)));
      byte[] differentClaim = "different-bytes-same-claim".getBytes(StandardCharsets.UTF_8);
      assertTrue(
          memory.store(
              NOTEBOOK_ID,
              DIGEST,
              differentClaim.length,
              new ByteArrayInputStream(differentClaim)));
      assertArrayEquals(PAYLOAD, memory.get(NOTEBOOK_ID, DIGEST).orElseThrow());
    }
  }

  private static String sha256Hex(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
