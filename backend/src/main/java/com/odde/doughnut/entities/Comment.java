package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.Optional;

@Getter
@Setter
public class Comment {
  private Integer id;
  private User author;
  private Timestamp createdAt;
  private String description;
  private Optional<Note> parentNote;
}
