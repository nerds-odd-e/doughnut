package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NotebookCreationRequest;
import com.odde.donut.controllers.dto.NotebookRealm;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookGit.NotebookGitBundleBuilder;
import com.odde.donut.services.notebookGit.NotebookGitBundleWriter;
import com.odde.donut.testability.GitBundleTestReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.BundleWriter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Shared notebook/bundle-crafting fixtures for the notebook Git-bundle download and proposal
 * controller tests ({@link NotebookGitBundleControllerTest}, {@link
 * NotebookGitProposalImportControllerTest}, {@link NotebookGitProposalAncestryControllerTest},
 * {@link NotebookGitProposalTreeShapeControllerTest}, {@link
 * NotebookGitProposalMarkdownFormatControllerTest}, and {@link
 * NotebookGitProposalPropertyValidationControllerTest}).
 */
abstract class NotebookGitBundleControllerTestBase extends NotebookControllerTestBase {

  Notebook createGitBackedNotebook() throws UnexpectedNoAccessRightException {
    NotebookCreationRequest request = new NotebookCreationRequest();
    request.setNewTitle("Git Backed Notebook For Bundle");
    NotebookRealm response = controller.createNotebook(request);
    return notebookRepository.findById(response.notebook().getId()).orElseThrow();
  }

  ResponseStatusException assertProposalRejectedWithoutMutatingBinding(
      Notebook notebook, String expectedHead, byte[] bundleBytes, HttpStatus expectedStatus)
      throws UnexpectedNoAccessRightException {
    NotebookGitBinding before =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.publishNotebookGitProposal(notebook, expectedHead, bundleBytes));

    assertThat(exception.getStatusCode(), equalTo(expectedStatus));

    NotebookGitBinding after =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    assertThat(after.getAcceptedGitObjectId(), equalTo(before.getAcceptedGitObjectId()));
    assertThat(after.getBundleBytes(), equalTo(before.getBundleBytes()));
    assertThat(after.getUpdatedAt(), equalTo(before.getUpdatedAt()));
    return exception;
  }

  /** One file's path, content bytes, and mode within a crafted proposal tree. */
  record ProposedFile(String path, byte[] contentBytes, FileMode mode) {
    ProposedFile(String path, String content, FileMode mode) {
      this(path, content.getBytes(StandardCharsets.UTF_8), mode);
    }

    ProposedFile(String path, String content) {
      this(path, content, FileMode.REGULAR_FILE);
    }

    /** For deliberately-invalid byte sequences (e.g. malformed UTF-8) that no String can hold. */
    ProposedFile(String path, byte[] contentBytes) {
      this(path, contentBytes, FileMode.REGULAR_FILE);
    }
  }

  static ObjectId commitOnTopOf(
      Repository repository, List<ObjectId> parents, String path, String content, String message)
      throws IOException {
    return commitOnTopOf(repository, parents, List.of(new ProposedFile(path, content)), message);
  }

  static ObjectId commitOnTopOf(
      Repository repository, List<ObjectId> parents, List<ProposedFile> files, String message)
      throws IOException {
    try (ObjectInserter inserter = repository.newObjectInserter()) {
      DirCache dirCache = DirCache.newInCore();
      DirCacheBuilder builder = dirCache.builder();
      List<ProposedFile> sortedByPath =
          files.stream().sorted(Comparator.comparing(ProposedFile::path)).toList();
      for (ProposedFile file : sortedByPath) {
        ObjectId blobId = inserter.insert(Constants.OBJ_BLOB, file.contentBytes());
        DirCacheEntry entry = new DirCacheEntry(file.path());
        entry.setFileMode(file.mode());
        entry.setObjectId(blobId);
        builder.add(entry);
      }
      builder.finish();
      ObjectId treeId = dirCache.writeTree(inserter);

      PersonIdent author =
          new PersonIdent("Proposer", "proposer@example.com", Instant.now(), ZoneOffset.UTC);
      CommitBuilder commitBuilder = new CommitBuilder();
      commitBuilder.setTreeId(treeId);
      commitBuilder.setParentIds(parents.toArray(new ObjectId[0]));
      commitBuilder.setAuthor(author);
      commitBuilder.setCommitter(author);
      commitBuilder.setMessage(message);
      ObjectId commitId = inserter.insert(commitBuilder);
      inserter.flush();
      return commitId;
    }
  }

  static byte[] bundleBytesForHead(Repository repository, ObjectId headId) throws IOException {
    RefUpdate refUpdate = repository.updateRef(Constants.R_HEADS + "main");
    refUpdate.setNewObjectId(headId);
    refUpdate.forceUpdate();

    BundleWriter bundleWriter = new BundleWriter(repository);
    bundleWriter.include(Constants.R_HEADS + "main", headId);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    bundleWriter.writeBundle(NullProgressMonitor.INSTANCE, out);
    return out.toByteArray();
  }

  /**
   * Testability-only: overwrites {@code notebook}'s accepted Git binding with a fresh root commit
   * built directly from {@code entries}, so proposal-gating tests can control the accepted tree's
   * exact shape without depending on the notebook's own note/folder content.
   */
  NotebookGitBinding seedAcceptedBinding(Notebook notebook, List<PortableTreeEntry> entries) {
    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    try (Repository repository =
        NotebookGitBundleBuilder.build(
            entries, "System", "system@example.com", "Seed content", Instant.now())) {
      NotebookGitBundleWriter.BundleWriteResult written = NotebookGitBundleWriter.write(repository);
      binding.setAcceptedGitObjectId(written.headObjectId());
      binding.setBundleBytes(written.bundleBytes());
    }
    return notebookGitBindingRepository.save(binding);
  }

  /** A bundle whose {@code main} is a single-parent child of {@code binding}'s accepted head. */
  byte[] proposalBundleBytes(NotebookGitBinding binding, List<ProposedFile> proposedFiles)
      throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId acceptedHead = GitBundleTestReader.fetchHead(repository, binding.getBundleBytes());
      ObjectId childCommit =
          commitOnTopOf(repository, List.of(acceptedHead), proposedFiles, "Proposal");
      return bundleBytesForHead(repository, childCommit);
    }
  }

  /**
   * A frontmatter-valid baseline tree: one typed note, one typed README - clears tree-shape and
   * reaches the typed-Markdown gate, for tests whose proposal must get past both.
   */
  static List<PortableTreeEntry> validBaselineEntries() {
    return List.of(
        new PortableTreeEntry("note.md", "---\ntype: Note\n---\noriginal content"),
        new PortableTreeEntry("README.md", "---\ntype: Readme\n---\nreadme original"));
  }
}
