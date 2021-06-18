package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import lombok.Getter;
import lombok.Setter;

public class ReviewPointViewedByUser {
    @Getter
    @Setter
    private ReviewPoint reviewPoint;
    @Getter
    @Setter
    private NoteViewedByUser noteViewedByUser;
    @Getter
    @Setter
    private LinkViewedByUser linkViewedByUser;

    public static ReviewPointViewedByUser getReviewPointViewedByUser(ReviewPoint reviewPoint, User entity) {
        ReviewPointViewedByUser result = new ReviewPointViewedByUser();
        result.setReviewPoint(reviewPoint);
        if (reviewPoint.getNote() != null) {
            result.setNoteViewedByUser(reviewPoint.getNote().jsonObjectViewedBy(entity));
        }
        else {
            LinkViewedByUser linkViewedByUser = new LinkViewedByUser();
            linkViewedByUser.setSourceNoteViewedByUser(reviewPoint.getLink().getSourceNote().jsonObjectViewedBy(entity));
            linkViewedByUser.setTargetNoteViewedByUser(reviewPoint.getLink().getTargetNote().jsonObjectViewedBy(entity));
            linkViewedByUser.setLinkTypeLabel(reviewPoint.getLink().getLinkTypeLabel());
            result.setLinkViewedByUser(linkViewedByUser);
        }
        return result;
    }

}
