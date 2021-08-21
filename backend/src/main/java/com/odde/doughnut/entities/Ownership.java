package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public Note createNotebook(User user, NoteContent noteContent) throws IOException {
      final Note note = new Note();
      note.updateNoteContent(noteContent, user);
      note.buildNotebookForHeadNote(this, user);
      return note;
    }
}
