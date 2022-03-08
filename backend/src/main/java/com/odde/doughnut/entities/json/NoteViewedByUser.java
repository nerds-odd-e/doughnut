package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.NoteAccessories;
import com.odde.doughnut.entities.TextContent;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NoteViewedByUser {
    @Getter
    @Setter
    private Integer id;

    @Getter
    @Setter
    private Optional<Integer> parentId;

    @Getter
    @Setter
    private String title;

    @Getter
    @Setter
    private String shortDescription;

    @Getter
    @Setter
    private Optional<String> notePicture;

    @Getter
    @Setter
    private Timestamp createdAt;

    @Getter
    @Setter
    private NoteAccessories noteAccessories;

    @Getter
    @Setter
    private Map<Link.LinkType, LinkViewed> links;

    @Getter
    @Setter
    private List<Integer> childrenIds;

    @Getter
    @Setter
    private TextContent textContent;

}
