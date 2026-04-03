package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Book;
import org.springframework.data.repository.CrudRepository;

public interface BookRepository extends CrudRepository<Book, Integer> {}
