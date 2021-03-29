package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notes_closure")
public class NotesClosureEntity {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "note_id", referencedColumnName = "id")
    @Getter @Setter private NoteEntity noteEntity;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "ancestor_id", referencedColumnName = "id")
    @Getter @Setter private NoteEntity ancestorEntity;

    @Getter
    @Setter
    private Integer depth;
}
