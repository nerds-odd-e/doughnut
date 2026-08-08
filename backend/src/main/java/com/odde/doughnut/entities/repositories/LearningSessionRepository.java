package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.LearningSession;
import com.odde.doughnut.entities.LearningSessionStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningSessionRepository extends JpaRepository<LearningSession, Integer> {

  List<LearningSession> findByUser_IdAndNotebook_IdAndStatus(
      Integer userId, Integer notebookId, LearningSessionStatus status);
}
