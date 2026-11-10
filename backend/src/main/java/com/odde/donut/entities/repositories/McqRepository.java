package com.odde.donut.entities.repositories;

import com.odde.donut.entities.Mcq;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface McqRepository extends CrudRepository<Mcq, Integer> {
  List<Mcq> findTop10ByNote_IdOrderByCreatedAtDescIdDesc(Integer noteId);
}
