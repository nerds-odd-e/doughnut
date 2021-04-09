package com.odde.doughnut.models;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Subscription;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.List;

public class SubscriptionModel extends ModelForEntity<Subscription> implements ReviewScope {
    public SubscriptionModel(Subscription sub, ModelFactoryService modelFactoryService) {
        super(sub, modelFactoryService);
    }

    @Override
    public List<Note> getNotesHaveNotBeenReviewedAtAll() {
        return modelFactoryService.noteRepository.findByAncestorWhereThereIsNoReviewPoint(entity.getUserEntity(), entity.getHeadNote());
    }

    @Override
    public int getNotesHaveNotBeenReviewedAtAllCount() {
        return modelFactoryService.noteRepository.countByAncestorWhereThereIsNoReviewPoint(entity.getUserEntity(), entity.getHeadNote());
    }

    @Override
    public List<Link> getLinksHaveNotBeenReviewedAtAll() {
        return modelFactoryService.linkRepository.findByAncestorWhereThereIsNoReviewPoint(entity.getUserEntity(), entity.getHeadNote());
    }

    public boolean needToLearnMoreToday(List<Integer> noteIds) {
        int count = modelFactoryService.noteRepository.countByAncestorAndInTheList(entity.getHeadNote(), noteIds);
        return count < entity.getDailyTargetOfNewNotes();
    }
}
