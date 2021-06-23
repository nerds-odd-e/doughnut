package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.Reviewing;
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
    @Getter
    @Setter
    private ReviewSetting reviewSetting;

    public static ReviewPointViewedByUser getReviewPointViewedByUser(ReviewPoint reviewPoint, User entity) {
        ReviewPointViewedByUser result = new ReviewPointViewedByUser();
        if (reviewPoint == null) return result;

        result.setReviewPoint(reviewPoint);
        if (reviewPoint.getNote() != null) {
            result.setNoteViewedByUser(reviewPoint.getNote().jsonObjectViewedBy(entity));
            result.setReviewSetting(getReviewSetting(reviewPoint.getNote()));
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

    private static ReviewSetting getReviewSetting(Note note) {
        if(note == null) {
            return null;
        }
        ReviewSetting reviewSetting = note.getMasterReviewSetting();
        if (reviewSetting == null) {
            reviewSetting = new ReviewSetting();
        }
        return reviewSetting;
    }

}
