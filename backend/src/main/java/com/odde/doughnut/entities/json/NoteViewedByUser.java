package com.odde.doughnut.entities.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.odde.doughnut.entities.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class NoteViewedByUser {
    public static final int TWELVE_HOURS_MILLISECONDS = 12 * 60 * 60 * 1000;

    public Boolean getIsRecentlyUpdated(Timestamp currentUTCTimestamp) {
        Timestamp lastUpdatedAt = this.note.getNoteContent().getUpdatedAt();
        Timestamp twelveHoursAgo = new Timestamp(currentUTCTimestamp.getTime() - TWELVE_HOURS_MILLISECONDS);
        return lastUpdatedAt.compareTo(twelveHoursAgo) >= 0;
    }

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
    @JsonIgnoreProperties({"headNote"})
    private Notebook notebook;
    @Getter
    @Setter
    private Map<Link.LinkType, LinkViewed> links;
    @Getter
    @Setter
    private NoteNavigation navigation;
    @Getter
    @Setter
    @JsonIgnoreProperties({"noteContent"})
    private List<Note> ancestors;
    @Getter
    @Setter
    @JsonIgnoreProperties({"noteContent"})
    private List<Note> children;
    @Getter
    @Setter
    private Boolean owns;
}
