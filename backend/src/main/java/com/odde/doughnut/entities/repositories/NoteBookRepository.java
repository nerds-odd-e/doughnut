package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.NoteBookEntity;
import com.odde.doughnut.entities.UserEntity;
import org.springframework.data.repository.CrudRepository;

public interface NoteBookRepository extends CrudRepository<NoteBookEntity, Integer> {
}
