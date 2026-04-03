package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Book;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface BookRepository extends CrudRepository<Book, Integer> {

  Optional<Book> findByNotebook_Id(Integer notebookId);
}
