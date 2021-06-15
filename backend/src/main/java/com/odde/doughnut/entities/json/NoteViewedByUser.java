package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

public class NoteViewedByUser {
    @Getter
    @Setter
    private Note note;
    @Getter
    @Setter
    private Map<Link.LinkType, LinkViewedByUser> links;
}
