package com.odde.doughnut.entities.json;

import lombok.Getter;
import lombok.Setter;

public class NoteViewedByUser {
    @Setter
    @Getter
    private NotePositionViewedByUser notePosition;

    @Setter
    @Getter
    private NoteViewedByUser1 noteItself;
}
