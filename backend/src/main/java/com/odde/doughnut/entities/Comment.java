package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Comment {
  private Integer id;
  private User author;
  private Timestamp createdAt;
  private String description;
  @JsonIgnore private Note parentNote;
}
