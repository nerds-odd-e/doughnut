package com.odde.doughnut.models;

import com.odde.doughnut.entities.LinkEntity;
import com.odde.doughnut.entities.Note;

import java.util.List;

public interface ReviewScope {
    List<Note> getNotesHaveNotBeenReviewedAtAll();

    int getNotesHaveNotBeenReviewedAtAllCount();

    List<LinkEntity> getLinksHaveNotBeenReviewedAtAll();
}
