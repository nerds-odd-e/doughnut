package com.odde.donut.entities.repositories;

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
    List<PortableTreeEntry> entries = List.of(new PortableTreeEntry("README.md", "Hello"));
    Instant commitTime = Instant.parse("2026-09-04T10:15:30Z");
    try (Repository gitRepository =
        NotebookGitBundleBuilder.build(
            entries, "Donut System", "system@donut.local", "Snapshot import", commitTime)) {
      return NotebookGitBundleWriter.write(gitRepository);
    }
  }

  @Test
  void persistsAndReloadsAcceptedBundleByNotebookId() throws IOException {
    Notebook notebook = makeMe.aNotebook().please();
    makeMe.entityPersister.flush();

    BundleWriteResult built = buildBundle();

    NotebookGitBinding binding = new NotebookGitBinding();
    binding.setNotebook(notebook);
    binding.setAcceptedGitObjectId(built.headObjectId());
    binding.setBundleBytes(built.bundleBytes());
    Timestamp now = makeMe.aTimestamp().please();
    binding.setCreatedAt(now);
    binding.setUpdatedAt(now);
    repository.save(binding);
    makeMe.entityPersister.flushAndClear();

    NotebookGitBinding reloaded = repository.findByNotebook_Id(notebook.getId()).orElseThrow();
    assertThat(reloaded.getAcceptedGitObjectId(), equalTo(built.headObjectId()));
    assertThat(reloaded.getBundleBytes(), equalTo(built.bundleBytes()));
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
