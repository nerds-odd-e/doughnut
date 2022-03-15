package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Ownership;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Id;

public class NotebookViewedByUser {
    @Id
    @Getter @Setter
    private Integer id;

    @Id
    @Getter @Setter
    private Integer headNoteId;

    @Id
    @Getter @Setter
    private Note headNote;

    @Getter @Setter
    private Boolean fromBazaar;

    @Getter @Setter private Ownership ownership;
}
