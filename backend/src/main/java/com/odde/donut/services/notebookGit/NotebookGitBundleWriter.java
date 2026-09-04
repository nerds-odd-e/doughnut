package com.odde.donut.services.notebookGit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.BundleWriter;

/**
 * Serializes a {@code refs/heads/main} repository built by {@link NotebookGitBundleBuilder} into
 * portable bundle bytes, alongside the head commit's Git object ID, for persistence.
 */
public final class NotebookGitBundleWriter {

  private NotebookGitBundleWriter() {}

  public record BundleWriteResult(String headObjectId, byte[] bundleBytes) {}

  public static BundleWriteResult write(Repository repository) {
    try {
      Ref mainRef = repository.exactRef(Constants.R_HEADS + "main");
      ObjectId headObjectId = mainRef.getObjectId();

      BundleWriter bundleWriter = new BundleWriter(repository);
      bundleWriter.include(Constants.R_HEADS + "main", headObjectId);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      bundleWriter.writeBundle(NullProgressMonitor.INSTANCE, out);

      return new BundleWriteResult(headObjectId.getName(), out.toByteArray());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
