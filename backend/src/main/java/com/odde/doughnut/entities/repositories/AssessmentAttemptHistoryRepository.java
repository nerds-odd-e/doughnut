package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.AssessmentAttemptHistory;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import java.sql.Timestamp;
import org.springframework.data.repository.CrudRepository;

public interface AssessmentAttemptHistoryRepository
    extends CrudRepository<AssessmentAttemptHistory, Integer> {

  int countAssessmentAttemptHistoriesByNotebookAndUserAndSubmittedAtBetween(
      Notebook notebook, User user, Timestamp beginSubmittedAt, Timestamp endSubmittedAt);
}
