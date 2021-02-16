package com.odde.doughnut.repositories;

import com.odde.doughnut.models.Note;
import org.springframework.data.repository.CrudRepository;

public interface NoteRepository extends CrudRepository<Note, Integer> {
}
