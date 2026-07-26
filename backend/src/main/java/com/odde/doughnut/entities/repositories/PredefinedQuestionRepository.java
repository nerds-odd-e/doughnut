package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.PredefinedQuestion;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface PredefinedQuestionRepository extends CrudRepository<PredefinedQuestion, Integer> {
  List<PredefinedQuestion> findByIdInAndNote_Id(List<Integer> ids, Integer noteId);
}
