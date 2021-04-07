package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "note_book")
public class NoteBookEntity {
    @Id @Getter @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;

    @OneToOne
    @JoinColumn(name = "creator_id")
    @JsonIgnore
    @Getter @Setter private UserEntity creatorEntity;

    @OneToOne
    @JoinColumn(name = "top_note_id")
    @JsonIgnore
    @Getter @Setter private NoteEntity topNoteEntity;

    @OneToOne
    @JoinColumn(name = "ownership_id")
    @JsonIgnore
    @Getter @Setter private OwnershipEntity ownershipEntity;

    @Column(name="skip_review_entirely")
    @Getter @Setter Boolean skipReviewEntirely = false;
}
