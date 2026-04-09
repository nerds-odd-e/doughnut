package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.NotebookGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotebookGroupRepository extends JpaRepository<NotebookGroup, Integer> {

  List<NotebookGroup> findByOwnership_Id(Integer ownershipId);
}
