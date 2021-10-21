package com.odde.doughnut.entities;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ownership")
public class Ownership {
    @Id @Getter @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;

    @OneToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    @Getter @Setter private User user;

    @OneToOne
    @JoinColumn(name = "circle_id")
    @Getter @Setter private Circle circle;

    @OneToMany(mappedBy = "ownership")
    @JsonIgnore
    @Getter @Setter private List<Notebook> notebooks = new ArrayList<>();

    public boolean ownsBy(User user) {
        if(this.user != null) {
            return this.user.equals(user);
        }
        return circle.getMembers().contains(user);
    }

    public Note createNotebook(User user, NoteContent noteContent, Timestamp currentUTCTimestamp) {
        final Note note = Note.createNote(user, noteContent, currentUTCTimestamp);
        note.buildNotebookForHeadNote(this, user);
      return note;
    }

}
