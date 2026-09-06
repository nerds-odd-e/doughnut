package com.odde.donut.services.notebookGit;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Imports a client-submitted proposal Git bundle into a fresh in-memory repository, reusing the
 * same {@code TransportBundleStream} fetch used to read back an accepted binding's bundle (see
 * {@code GitBundleTestReader}). Unlike that test-only reader, a proposal comes from an untrusted
 * client, so any failure to produce a complete, usable {@code refs/heads/main} - corrupt bytes, a
 * missing ref, or a bundle that doesn't actually carry every object it advertises reachability to -
 * is converted into an actionable {@link ResponseStatusException} rather than left to propagate as
 * a raw JGit exception.
 */
public final class NotebookGitProposalImporter {

  private NotebookGitProposalImporter() {}

  /** The fresh in-memory repository the bundle was imported into, and its resolved main head. */
  public record ImportedProposal(Repository repository, ObjectId mainHead) {}

  public static ImportedProposal importMainHead(byte[] bundleBytes) {
    try {
      NotebookGitBundleImporter.ImportedBundle imported =
          NotebookGitBundleImporter.importMainHead(bundleBytes, "proposal-bundle");
      return new ImportedProposal(imported.repository(), imported.mainHead());
    } catch (RuntimeException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Proposal bundle is unreadable or does not carry a complete refs/heads/main.",
          e);
    }
  }
}
