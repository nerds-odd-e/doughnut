package com.odde.doughnut.entities.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteContent;
import com.odde.doughnut.entities.Ownership;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
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
    @Getter
    @Setter
    @JsonIgnoreProperties({"noteContent"})
    private List<Note> ancestors;
    @Getter
    @Setter
    private Ownership ownership;
}
