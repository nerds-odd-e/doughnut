package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

public class NoteViewedByUser {
    public static class NoteNavigation {
        @Getter @Setter private Integer previousSiblingId;
        @Getter @Setter private Integer previousId;
        @Getter @Setter private Integer nextId;
        @Getter @Setter private Integer nextSiblingId;
    }
    @Getter
    @Setter
    private Note note;
    @Getter
    @Setter
    private Map<Link.LinkType, LinkViewedByUser> links;
    @Getter
    @Setter
    private NoteNavigation navigation;
}
