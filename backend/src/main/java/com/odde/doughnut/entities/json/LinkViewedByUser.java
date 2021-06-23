package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.User;
import lombok.Getter;
import lombok.Setter;

public class LinkViewedByUser {
    @Getter
    @Setter
    private Integer id;
    @Getter
    @Setter
    private NoteViewedByUser sourceNoteViewedByUser;
    @Getter
    @Setter
    private String linkTypeLabel;
    @Getter
    @Setter
    private Integer linkTypeId;
    @Getter
    @Setter
    private NoteViewedByUser targetNoteViewedByUser;

    public static LinkViewedByUser from(Link link, User entity) {
        LinkViewedByUser linkViewedByUser = new LinkViewedByUser();
        linkViewedByUser.setSourceNoteViewedByUser(link.getSourceNote().jsonObjectViewedBy(entity));
        linkViewedByUser.setTargetNoteViewedByUser(link.getTargetNote().jsonObjectViewedBy(entity));
        linkViewedByUser.setLinkTypeLabel(link.getLinkTypeLabel());
        linkViewedByUser.setLinkTypeId(link.getLinkType().id);
        linkViewedByUser.setId(link.getId());
        return linkViewedByUser;
    }

}
