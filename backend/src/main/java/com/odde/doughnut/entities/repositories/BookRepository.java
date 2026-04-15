package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Book;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends CrudRepository<Book, Integer> {

  Optional<Book> findByNotebook_Id(Integer notebookId);

  @Query("select b.notebook.id from Book b where b.notebook.id in :ids")
  List<Integer> findNotebookIdsWithAttachedBooksIn(@Param("ids") Collection<Integer> ids);
}
