package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.NotebookAiAssistant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotebookAiAssistantRepository extends JpaRepository<NotebookAiAssistant, Integer> {
  NotebookAiAssistant findByNotebookId(Integer notebookId);
}
