package com.odde.doughnut.factoryServices;

import com.odde.doughnut.entities.Comment;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CommentRepository {

  public List<Comment> findByNoteId(Integer id) {
    return List.of(new Comment());
  }
}
