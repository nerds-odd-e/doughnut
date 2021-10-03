package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.models.UserModel;
import lombok.Getter;
import lombok.Setter;

public class ReviewPointViewedByUser {
    @Getter
    @Setter
    private ReviewPoint reviewPoint;
    @Getter
    @Setter
    private NoteWithPosition noteWithPosition;
    @Getter
    @Setter
    private LinkViewedByUser linkViewedByUser;
    @Getter
    @Setter
    private ReviewSetting reviewSetting;
    @Getter
    @Setter
    private Integer remainingInitialReviewCountForToday;

    public static ReviewPointViewedByUser from(ReviewPoint reviewPoint, UserModel user) {
        ReviewPointViewedByUser result = new ReviewPointViewedByUser();
        if (reviewPoint == null) return result;

        result.setReviewPoint(reviewPoint);
        if (reviewPoint.getNote() != null) {
            result.setNoteWithPosition(reviewPoint.getNote().jsonObjectViewedBy(user.getEntity()));
            result.setReviewSetting(getReviewSetting(reviewPoint.getNote()));
        }
        else {
            Link link = reviewPoint.getLink();
            LinkViewedByUser linkViewedByUser = LinkViewedByUser.from(link, user);
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
