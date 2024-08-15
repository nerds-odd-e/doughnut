package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.AssessmentAttempt;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import java.sql.Timestamp;
import org.springframework.data.repository.CrudRepository;

public interface AssessmentAttemptRepository extends CrudRepository<AssessmentAttempt, Integer> {

  int countByNotebookAndUserAndSubmittedAtBetween(
      Notebook notebook, User user, Timestamp beginSubmittedAt, Timestamp endSubmittedAt);

  AssessmentAttempt findFirstByNotebookAndUserAndSubmittedAt(
      Notebook notebook, User user, Timestamp submittedAt);
}
