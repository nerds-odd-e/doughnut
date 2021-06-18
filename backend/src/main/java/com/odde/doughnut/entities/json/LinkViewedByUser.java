package com.odde.doughnut.entities.json;

import lombok.Getter;
import lombok.Setter;

public class LinkViewedByUser {
    @Getter
    @Setter
    private NoteViewedByUser sourceNoteViewedByUser;
    @Getter
    @Setter
    private String linkTypeLabel;
    @Getter
    @Setter
    private NoteViewedByUser targetNoteViewedByUser;
}
