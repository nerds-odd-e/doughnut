package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "notes_closure")
public class NotesClosure {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "note_id", referencedColumnName = "id")
    @Getter @Setter private Note note;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "ancestor_id", referencedColumnName = "id")
    @Getter @Setter private Note ancestorEntity;

    @Getter
    @Setter
    private Integer depth;
}
