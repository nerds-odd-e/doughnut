package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "bazaar_note")
public class BazaarNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;

    @ManyToOne
    @JoinColumn(name = "note_id", referencedColumnName = "id")
    @Getter @Setter private NoteEntity note;
}
