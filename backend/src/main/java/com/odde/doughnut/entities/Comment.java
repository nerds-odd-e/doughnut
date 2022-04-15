package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.json.CommentCreation;
import com.odde.doughnut.models.UserModel;
import java.sql.Timestamp;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Comment {
  private Integer id;
  private User author;
  private Timestamp createdAt;
  private String description;

  @JsonIgnore private Integer noteId;
  @JsonIgnore private Note parentNote;

  public static Comment from(Note note, CommentCreation commentCreation, UserModel userModel) {
    Comment comment = new Comment();
    comment.setAuthor(userModel.getEntity());
    comment.setCreatedAt(Timestamp.from(Instant.now()));
    comment.setDescription(commentCreation.getDescription());
    comment.setNoteId(note.getId());
    comment.setParentNote(note);
    return comment;
  }
}
