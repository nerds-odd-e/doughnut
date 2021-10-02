package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteContent;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class NoteViewedByUser1 {
    @Getter
    @Setter
    private Integer id;

    @Getter
    @Setter
    private Integer parentId;

    @Getter
    @Setter
    private String title;

    @Getter
    @Setter
    private String notePicture;

    @Getter
    @Setter
    private Timestamp createdAt;

    @Getter
    @Setter
    private NoteContent noteContent;

    @Getter
    @Setter
    private Map<Link.LinkType, LinkViewed> links;

    @Getter
    @Setter
    private List<Integer> childrenIds;

}
