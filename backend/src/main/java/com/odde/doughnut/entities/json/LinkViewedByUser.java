package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.models.UserModel;
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
    private Integer typeId;
    @Getter
    @Setter
    private NoteViewedByUser targetNoteViewedByUser;
    @Getter
    @Setter
    private Boolean readonly;

    public static LinkViewedByUser from(Link link, UserModel user) {
        LinkViewedByUser linkViewedByUser = new LinkViewedByUser();
        linkViewedByUser.setSourceNoteViewedByUser(link.getSourceNote().jsonObjectViewedBy(user.getEntity()));
        linkViewedByUser.setTargetNoteViewedByUser(link.getTargetNote().jsonObjectViewedBy(user.getEntity()));
        linkViewedByUser.setLinkTypeLabel(link.getLinkTypeLabel());
        linkViewedByUser.setTypeId(link.getLinkType().id);
        linkViewedByUser.setId(link.getId());
        linkViewedByUser.setReadonly(!user.getAuthorization().hasFullAuthority(link.getSourceNote()));
        return linkViewedByUser;
    }

}
