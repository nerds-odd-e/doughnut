package com.odde.doughnut.entities.json;

import lombok.Getter;
import lombok.Setter;

public class NoteWithPosition {
    @Setter
    @Getter
    private NotePositionViewedByUser notePosition;

    @Setter
    @Getter
    private NoteSphere note;
}
