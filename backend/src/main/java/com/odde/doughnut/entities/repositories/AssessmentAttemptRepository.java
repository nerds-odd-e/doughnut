package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.AssessmentAttempt;
import com.odde.doughnut.entities.User;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface AssessmentAttemptRepository extends CrudRepository<AssessmentAttempt, Integer> {
  List<AssessmentAttempt> findAllByUser(User user);
}
