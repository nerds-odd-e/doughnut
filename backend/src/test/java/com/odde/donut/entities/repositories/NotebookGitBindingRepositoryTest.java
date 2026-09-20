package com.odde.donut.entities.repositories;

import static com.odde.donut.services.notebookExport.PortableTreeEntry.ofText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookGit.NotebookGitBundleBuilder;
import com.odde.donut.services.notebookGit.NotebookGitBundleWriter;
import com.odde.donut.services.notebookGit.NotebookGitBundleWriter.BundleWriteResult;
import com.odde.donut.testability.MakeMe;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotebookGitBindingRepositoryTest {

  @Autowired MakeMe makeMe;
  @Autowired NotebookGitBindingRepository repository;
  @Autowired JdbcTemplate jdbcTemplate;

  private static BundleWriteResult buildBundle() throws IOException {
    List<PortableTreeEntry> entries = List.of(ofText("README.md", "Hello"));
    Instant commitTime = Instant.parse("2026-09-04T10:15:30Z");
    try (Repository gitRepository =
        NotebookGitBundleBuilder.build(
            entries, "Donut System", "system@donut.local", "Snapshot import", commitTime)) {
      return NotebookGitBundleWriter.write(gitRepository);
    }
  }

  private NotebookGitBinding persistBinding(Notebook notebook) throws IOException {
    BundleWriteResult built = buildBundle();
    NotebookGitBinding binding = new NotebookGitBinding();
    binding.setNotebook(notebook);
    binding.setAcceptedGitObjectId(built.headObjectId());
    binding.setBundleBytes(built.bundleBytes());
    Timestamp now = makeMe.aTimestamp().please();
    binding.setCreatedAt(now);
    binding.setUpdatedAt(now);
    NotebookGitBinding saved = repository.save(binding);
    makeMe.entityPersister.flushAndClear();
    return saved;
  }

  @Test
  void persistsAndReloadsAcceptedBundleByNotebookId() throws IOException {
    Notebook notebook = makeMe.aNotebook().please();
    makeMe.entityPersister.flush();

    NotebookGitBinding saved = persistBinding(notebook);

    NotebookGitBinding reloaded = repository.findByNotebook_Id(notebook.getId()).orElseThrow();
    assertThat(reloaded.getAcceptedGitObjectId(), equalTo(saved.getAcceptedGitObjectId()));
    assertThat(reloaded.getBundleBytes(), equalTo(saved.getBundleBytes()));
  }

  @Test
  void cascadeDeletesNativeObjectStoreRowsWhenBindingIsDeleted() throws IOException {
    Notebook notebook = makeMe.aNotebook().please();
    makeMe.entityPersister.flush();

    NotebookGitBinding saved = persistBinding(notebook);

    jdbcTemplate.update(
        """
        INSERT INTO notebook_git_accepted_object
          (notebook_git_binding_id, git_object_id, object_type, object_bytes)
        VALUES (?, ?, ?, ?)
        """,
        saved.getId(),
        "c".repeat(40),
        1,
        new byte[] {1, 2, 3});
    assertThat(countNativeObjectStoreRows(saved.getId()), equalTo(1L));

    repository.deleteById(saved.getId());
    makeMe.entityPersister.flush();

    assertThat(countNativeObjectStoreRows(saved.getId()), equalTo(0L));
  }

  private long countNativeObjectStoreRows(Integer bindingId) {
    Long count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notebook_git_accepted_object WHERE notebook_git_binding_id = ?",
            Long.class,
            bindingId);
    return count;
  }

  @Test
  void rejectsSecondBindingForSameNotebook() {
    Notebook notebook = makeMe.aNotebook().please();
    makeMe.entityPersister.flush();

    jdbcTemplate.update(
        """
        INSERT INTO notebook_git_binding
          (notebook_id, accepted_git_object_id, bundle_bytes, created_at, updated_at)
        VALUES (?, ?, ?, NOW(3), NOW(3))
        """,
        notebook.getId(),
        "a".repeat(40),
        new byte[] {1, 2, 3});

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbcTemplate.update(
                """
                INSERT INTO notebook_git_binding
                  (notebook_id, accepted_git_object_id, bundle_bytes, created_at, updated_at)
                VALUES (?, ?, ?, NOW(3), NOW(3))
                """,
                notebook.getId(),
                "b".repeat(40),
                new byte[] {4, 5, 6}));
  }
}
