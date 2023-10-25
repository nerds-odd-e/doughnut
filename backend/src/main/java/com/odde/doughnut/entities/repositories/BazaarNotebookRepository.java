package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.BazaarNotebook;
import com.odde.doughnut.entities.Notebook;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface BazaarNotebookRepository extends CrudRepository<BazaarNotebook, Integer> {
  BazaarNotebook findByNotebook(Notebook notebook);

  @Query(
      value =
          "SELECT bazaar_notebook.* from bazaar_notebook JOIN notebook on notebook.id = bazaar_notebook.notebook_id WHERE notebook.deleted_at IS NULL ",
      nativeQuery = true)
  List<BazaarNotebook> findAllNonDeleted();
}
