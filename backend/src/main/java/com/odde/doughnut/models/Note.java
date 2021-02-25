package com.odde.doughnut.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "note")
public class Note {
  @Id @Getter @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
  @Getter @Setter private String title;
  @Getter @Setter private String description;
  @Getter @Setter private String picture;

  @ManyToOne
  @JoinColumn(name = "parent_id")
  @JsonIgnore
  @Getter @Setter private Note parentNote;

  @OneToMany(mappedBy = "parentNote", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonIgnore
  @Getter private List<Note> children = new ArrayList<>();

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "user_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter @Setter private User user;

  @Column(name="created_datetime")
  @Getter @Setter private Date createdDatetime;

  @Column(name="updated_datetime")
  @Getter @Setter private Date updatedDatetime;

  @JoinTable(name = "link", joinColumns = {
          @JoinColumn(name = "source_id", referencedColumnName = "id", nullable = false)}, inverseJoinColumns = {
            @JoinColumn(name = "target_id", referencedColumnName = "id", nullable = false)
          })
  @ManyToMany
  @JsonIgnoreProperties("targetNotes")
  @Getter @Setter private List<Note> targetNotes = new ArrayList<>();

  public void linkToNote(Note targetNote) {
    this.targetNotes.add(targetNote);
  }

  public void addChild(Note note) {
      note.setParentNote(this);
      getChildren().add(note);
  }
}


