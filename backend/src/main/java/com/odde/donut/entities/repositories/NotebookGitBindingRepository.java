package com.odde.donut.entities.repositories;

import com.odde.donut.entities.NotebookGitBinding;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface NotebookGitBindingRepository extends CrudRepository<NotebookGitBinding, Integer> {

  Optional<NotebookGitBinding> findByNotebook_Id(Integer notebookId);
}
