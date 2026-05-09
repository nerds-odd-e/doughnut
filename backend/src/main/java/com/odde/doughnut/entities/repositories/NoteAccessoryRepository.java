package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.NoteAccessory;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface NoteAccessoryRepository extends CrudRepository<NoteAccessory, Integer> {

  Optional<NoteAccessory> findByNote_Id(Integer noteId);
}
