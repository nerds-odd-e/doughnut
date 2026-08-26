package com.odde.donut.entities.repositories;

import com.odde.donut.entities.NotebookGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotebookGroupRepository extends JpaRepository<NotebookGroup, Integer> {

  List<NotebookGroup> findByOwnership_Id(Integer ownershipId);
}
