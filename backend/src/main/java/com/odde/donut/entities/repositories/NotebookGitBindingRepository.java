package com.odde.donut.entities.repositories;

import com.odde.donut.entities.NotebookGitAttachmentRepresentation;
import com.odde.donut.entities.NotebookGitBinding;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface NotebookGitBindingRepository extends CrudRepository<NotebookGitBinding, Integer> {

  Optional<NotebookGitBinding> findByNotebook_Id(Integer notebookId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT binding FROM NotebookGitBinding binding WHERE binding.notebook.id = :notebookId")
  Optional<NotebookGitBinding> findByNotebookIdForUpdate(@Param("notebookId") Integer notebookId);

  @Query(
      "SELECT binding.notebook.id FROM NotebookGitBinding binding"
          + " WHERE binding.attachmentRepresentation = :representation")
  List<Integer> findNotebookIdsByAttachmentRepresentation(
      @Param("representation") NotebookGitAttachmentRepresentation representation);
}
