package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.sql.Timestamp;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "comment")
public class Comment {
  @Id
  @Getter
  @JsonIgnore
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Getter
  @Setter
  @Column(name = "text")
  String text;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "note_id")
  @Getter
  @Setter
  Note note;

  @Column(name = "created_at")
  @Getter
  @Setter
  @JsonIgnore
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());



}
