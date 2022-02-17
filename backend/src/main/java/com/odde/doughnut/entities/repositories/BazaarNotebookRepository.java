package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.BazaarNotebook;
import com.odde.doughnut.entities.Notebook;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.sql.Timestamp;
import java.util.List;

public interface BazaarNotebookRepository extends CrudRepository<BazaarNotebook, Integer> {
    BazaarNotebook findByNotebook(Notebook notebook);
    @Query( value = "SELECT * from bazaar_notebook JOIN notebook on notebook.id = bazaar_notebook.notebook_id WHERE notebook.deleted_at IS NULL ", nativeQuery = true)
    List<BazaarNotebook> findAllNonDeleted();
}
