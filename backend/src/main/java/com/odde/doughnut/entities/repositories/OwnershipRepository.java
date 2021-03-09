package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.LinkEntity;
import com.odde.doughnut.entities.OwnershipEntity;
import org.springframework.data.repository.CrudRepository;

public interface OwnershipRepository extends CrudRepository<OwnershipEntity, Integer> {
}
