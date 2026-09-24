package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitCommitBuilder;
import com.odde.donut.services.notebookGit.NotebookGitTreeContent;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.NotebookGitAcceptedHistoryFixture;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Committed fixtures for a binding's accepted history in the native object store: seeding an exact
 * history, and removing or counting its stored objects.
 */
abstract class NotebookGitAcceptedObjectStoreTestSupport
    extends NotebookGitCommitFixtureTestSupport {

  @Autowired PlatformTransactionManager transactionManager;
  @Autowired DataSource dataSource;

  /** Seeds accepted history from exact tree entries (disjoint from notebook content). */
  NotebookGitBinding seedAcceptedBinding(Notebook notebook, List<PortableTreeEntry> entries) {
    try (Repository seeded =
        NotebookGitCommitBuilder.build(
            NotebookGitTreeContent.of(entries),
            "System",
            "system@example.com",
            "Seed content",
            Instant.now())) {
      return seedAcceptedHistory(
          notebook, seeded, NotebookGitAcceptedHistoryFixture.mainHeadOf(seeded));
    }
  }

  /** Seeds accepted history from an arbitrary repository tip into the native object store. */
  NotebookGitBinding seedAcceptedHistory(Notebook notebook, Repository source, ObjectId head) {
    NotebookGitBinding binding =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    binding.setAcceptedGitObjectId(head.name());
    NotebookGitBinding saved = notebookGitBindingRepository.save(binding);
    inCommittedTransaction(
        transactionManager,
        () ->
            NotebookGitAcceptedHistoryFixture.seedNativeObjectStore(
                dataSource, saved.getId(), source, head));
    return saved;
  }

  void deleteNativeObjectStoreRow(Integer bindingId, String gitObjectId) {
    inCommittedTransaction(
        transactionManager,
        () ->
            entityManager
                .createNativeQuery(
                    "DELETE FROM notebook_git_accepted_object WHERE notebook_git_binding_id ="
                        + " :bindingId AND git_object_id = :gitObjectId")
                .setParameter("bindingId", bindingId)
                .setParameter("gitObjectId", gitObjectId)
                .executeUpdate());
  }

  long countNativeObjectStoreRows(Integer bindingId) {
    return inCommittedTransaction(
        transactionManager,
        () ->
            ((Number)
                    entityManager
                        .createNativeQuery(
                            "SELECT COUNT(*) FROM notebook_git_accepted_object "
                                + "WHERE notebook_git_binding_id = :bindingId")
                        .setParameter("bindingId", bindingId)
                        .getSingleResult())
                .longValue());
  }
}
