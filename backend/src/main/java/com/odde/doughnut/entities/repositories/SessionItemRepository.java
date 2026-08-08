package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.SessionItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionItemRepository extends JpaRepository<SessionItem, Integer> {

  List<SessionItem> findByLearningSession_Id(Integer learningSessionId);
}
