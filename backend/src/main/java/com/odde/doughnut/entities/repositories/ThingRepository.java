package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Thing;
import org.springframework.data.repository.CrudRepository;

public interface ThingRepository extends CrudRepository<Thing, Integer> {}
