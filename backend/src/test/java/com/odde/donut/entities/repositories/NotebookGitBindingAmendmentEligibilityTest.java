package com.odde.donut.entities.repositories;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static com.odde.donut.testability.CommittedUserCleanup.deleteByUserExternalIdentifierLike;
import static com.odde.donut.testability.NotebookGitBindingAmendmentFixture.markEligible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookGit.NotebookGitBundleBuilder;
import com.odde.donut.services.notebookGit.NotebookGitBundleWriter;
import com.odde.donut.services.notebookGit.NotebookGitBundleWriter.BundleWriteResult;
import com.odde.donut.testability.MakeMe;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NotebookGitBindingAmendmentEligibilityTest {

  private static final String FIXTURE_PREFIX = "notebook-git-binding-amendment-";

  @Autowired MakeMe makeMe;
  @Autowired NotebookGitBindingRepository repository;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired EntityManager entityManager;

  @AfterEach
  void cleanupCommittedFixture() {
    inCommittedTransaction(
        transactionManager,
        () -> deleteByUserExternalIdentifierLike(entityManager, FIXTURE_PREFIX + "%"));
  }

  @Test
  void existingBindingLoadsWithAbsentEligibility() throws IOException {
    BundleWriteResult built = buildBundle();
    Integer notebookId =
        inCommittedTransaction(
            transactionManager,
            () -> {
              User user = makeMe.aUser(FIXTURE_PREFIX + UUID.randomUUID()).please();
              Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
              persistBinding(notebook, built);
              return notebook.getId();
            });

    NotebookGitBinding reloaded =
        inCommittedTransaction(
            transactionManager, () -> repository.findByNotebook_Id(notebookId).orElseThrow());

    assertThat(reloaded.getAmendmentHead(), nullValue());
    assertThat(reloaded.getAmendmentNoteId(), nullValue());
    assertThat(reloaded.getAmendmentLastChangedAt(), nullValue());
  }

  @Test
  void persistsAndReloadsEligibleAmendmentMetadata() throws IOException {
    Instant lastChanged = Instant.parse("2026-09-08T10:00:00Z");
    Integer noteId = 42;
    BundleWriteResult built = buildBundle();

    Integer notebookId =
        inCommittedTransaction(
            transactionManager,
            () -> {
              User user = makeMe.aUser(FIXTURE_PREFIX + UUID.randomUUID()).please();
              Notebook notebook = makeMe.aNotebook().creatorAndOwner(user).please();
              NotebookGitBinding binding = persistBinding(notebook, built);
              markEligible(binding, noteId, lastChanged);
              repository.save(binding);
              return notebook.getId();
            });

    NotebookGitBinding reloaded =
        inCommittedTransaction(
            transactionManager, () -> repository.findByNotebook_Id(notebookId).orElseThrow());

    assertThat(reloaded.getAmendmentHead(), equalTo(built.headObjectId()));
    assertThat(reloaded.getAmendmentNoteId(), equalTo(noteId));
    assertThat(reloaded.getAmendmentLastChangedAt().toInstant(), equalTo(lastChanged));
  }

  private NotebookGitBinding persistBinding(Notebook notebook, BundleWriteResult built) {
    NotebookGitBinding binding = new NotebookGitBinding();
    binding.setNotebook(notebook);
    binding.setAcceptedGitObjectId(built.headObjectId());
    binding.setBundleBytes(built.bundleBytes());
    Timestamp now = makeMe.aTimestamp().please();
    binding.setCreatedAt(now);
    binding.setUpdatedAt(now);
    return repository.save(binding);
  }

  private static BundleWriteResult buildBundle() throws IOException {
    List<PortableTreeEntry> entries = List.of(new PortableTreeEntry("README.md", "Hello"));
    Instant commitTime = Instant.parse("2026-09-04T10:15:30Z");
    try (Repository gitRepository =
        NotebookGitBundleBuilder.build(
            entries, "Donut System", "system@donut.local", "Snapshot import", commitTime)) {
      return NotebookGitBundleWriter.write(gitRepository);
    }
  }
}
