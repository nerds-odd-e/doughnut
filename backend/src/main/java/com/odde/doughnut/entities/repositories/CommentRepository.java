package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Comment;
import com.odde.doughnut.entities.Note;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface CommentRepository extends CrudRepository<Comment, Integer> {

  @Query(value = "SELECT * from comment where note_id=:note", nativeQuery = true)
  List<Comment> findAllByNote(Note note);
}
