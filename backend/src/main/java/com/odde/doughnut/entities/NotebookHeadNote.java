package com.odde.doughnut.entities;

import javax.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "notebook_head_note")
public class NotebookHeadNote {
  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
}
