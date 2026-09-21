package com.odde.donut.entities.repositories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.testability.MakeMe;
import java.sql.Timestamp;
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

  /** A persisted binding with the minimum every column requires. */
  private NotebookGitBinding persistBinding(Notebook notebook) {
    NotebookGitBinding binding = new NotebookGitBinding();
    binding.setNotebook(notebook);
    binding.setAcceptedGitObjectId("a".repeat(40));
    Timestamp now = makeMe.aTimestamp().please();
    binding.setCreatedAt(now);
    binding.setUpdatedAt(now);
    NotebookGitBinding saved = repository.save(binding);
    makeMe.entityPersister.flushAndClear();
    return saved;
  }

  @Test
  void cascadeDeletesNativeObjectStoreRowsWhenBindingIsDeleted() {
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
          (notebook_id, accepted_git_object_id, created_at, updated_at)
        VALUES (?, ?, NOW(3), NOW(3))
        """,
        notebook.getId(),
        "a".repeat(40));

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbcTemplate.update(
                """
                INSERT INTO notebook_git_binding
                  (notebook_id, accepted_git_object_id, created_at, updated_at)
                VALUES (?, ?, NOW(3), NOW(3))
                """,
                notebook.getId(),
                "b".repeat(40)));
  }
}
