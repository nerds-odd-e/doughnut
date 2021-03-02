package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.entities.UserEntity;
import org.springframework.data.repository.CrudRepository;

public interface ReviewPointRepository extends CrudRepository<ReviewPointEntity, Integer> {
    ReviewPointEntity findByNoteEntity(NoteEntity noteEntity);
}
