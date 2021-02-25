package com.odde.doughnut.repositories;

import com.odde.doughnut.models.BazaarNote;
import com.odde.doughnut.models.Link;
import org.springframework.data.repository.CrudRepository;

public interface BazaarNoteRepository extends CrudRepository<BazaarNote, Integer> {
}
