package com.odde.donut.services.notebookGit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.BundleWriter;

/**
 * Serializes a {@code refs/heads/main} repository built by {@link NotebookGitCommitBuilder} into
 * portable bundle bytes for transport (download and cloning).
 */
public final class NotebookGitBundleWriter {

  private NotebookGitBundleWriter() {}

  public static byte[] write(Repository repository) {
    try (ObjectReader reader = repository.newObjectReader()) {
      return write(repository, reader);
    }
  }

  /** Writes {@code repository}'s {@code main} head, reading its objects through {@code reader}. */
  static byte[] write(Repository repository, ObjectReader reader) {
    try {
      Ref mainRef = repository.exactRef(Constants.R_HEADS + "main");
      ObjectId headObjectId = mainRef.getObjectId();

      BundleWriter bundleWriter = new BundleWriter(reader);
      bundleWriter.include(Constants.R_HEADS + "main", headObjectId);
      // Without an included HEAD, a system `git clone` of this bundle names the checked-out
      // branch after the cloning machine's own `init.defaultBranch` (e.g. "master"), not "main".
      bundleWriter.include(Constants.HEAD, headObjectId);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      bundleWriter.writeBundle(NullProgressMonitor.INSTANCE, out);

      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
