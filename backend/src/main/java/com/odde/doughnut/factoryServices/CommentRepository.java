package com.odde.doughnut.factoryServices;

import com.odde.doughnut.entities.Comment;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CommentRepository {

  private List<Comment> comments = new ArrayList<>();

  public List<Comment> findByNoteId(Integer noteId) {
    return this.comments.stream()
        .filter(comment -> comment.getNoteId() == noteId)
        .collect(Collectors.toList());
  }

  public void save(Comment comment) {
    this.comments.add(comment);
  }
}
