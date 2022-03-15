package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.UserModel;
import lombok.Getter;
import lombok.Setter;

public class LinkViewedByUser {
    @Getter
    @Setter
    private Integer id;
    @Getter
    @Setter
    private NoteWithPosition sourceNoteWithPosition;
    @Getter
    @Setter
    private String linkTypeLabel;
    @Getter
    @Setter
    private Integer typeId;
    @Getter
    @Setter
    private NoteWithPosition targetNoteWithPosition;
    @Getter
    @Setter
    private Boolean readonly;

    public static LinkViewedByUser from(Link link, UserModel user) {
        LinkViewedByUser linkViewedByUser = new LinkViewedByUser();
        User userEntity = user.getEntity();
        Note sourceNote = link.getSourceNote();
        Note targetNote = link.getTargetNote();
        linkViewedByUser.setSourceNoteWithPosition(new NoteViewer(userEntity, sourceNote).jsonNoteWithPosition(sourceNote));
        linkViewedByUser.setTargetNoteWithPosition(new NoteViewer(userEntity, targetNote).jsonNoteWithPosition(targetNote));
        linkViewedByUser.setLinkTypeLabel(link.getLinkTypeLabel());
        linkViewedByUser.setTypeId(link.getLinkType().id);
        linkViewedByUser.setId(link.getId());
        linkViewedByUser.setReadonly(!user.getAuthorization().hasFullAuthority(link.getSourceNote()));
        return linkViewedByUser;
    }

}
