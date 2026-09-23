package com.odde.donut.services.notebookAttachment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Immutable notebook-scoped attachment content addressed by SHA-256 digest. Upload streams are
 * counted and hashed before durable storage; mismatched or interrupted attempts leave no verified
 * content and do not overwrite an existing verified object.
 */
public interface NotebookAttachmentContent {

  /**
   * Streams {@code content}, verifies byte count and SHA-256 against the claimed values, then
   * stores under a notebook-scoped digest key. Returns {@code true} when verified content is
   * present afterward (newly stored or already present). Returns {@code false} when the stream does
   * not match; nothing verified is written. A same-digest retry keeps existing verified bytes.
   */
  boolean store(Integer notebookId, String sha256Hex, long size, InputStream content)
      throws IOException;

  /** Returns verified bytes for the notebook-scoped digest, if present. */
  Optional<byte[]> get(Integer notebookId, String sha256Hex);
}
