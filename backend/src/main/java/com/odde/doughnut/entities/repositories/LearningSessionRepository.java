package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.LearningSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningSessionRepository extends JpaRepository<LearningSession, Integer> {

  List<LearningSession> findByUser_IdAndNotebook_Id(Integer userId, Integer notebookId);
}
