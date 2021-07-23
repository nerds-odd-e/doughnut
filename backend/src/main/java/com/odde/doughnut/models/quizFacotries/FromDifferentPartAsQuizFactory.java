package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.UserModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FromDifferentPartAsQuizFactory extends FromSamePartAsQuizFactory {
    private Link cachedAnswerLink = null;
    private List<Note> cachedFillingOptions = null;

    public FromDifferentPartAsQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        super(servant, reviewPoint);
    }

    @Override
    protected Link getAnswerLink() {
        if (cachedAnswerLink == null) {
            cachedAnswerLink = link.getRemoteCousinOfDifferentCategory(reviewPoint.getUser())
                    .map(servant.randomizer::chooseOneRandomly)
                    .orElse(null);
        }
        return cachedAnswerLink;
    }

    @Override
    public boolean isValidQuestion() {
        return generateAnswerNote() != null && generateFillingOptions().size() > 0;
    }

    @Override
    public List<ReviewPoint> getViceReviewPoints() {
        List<ReviewPoint> result = new ArrayList<>();
        UserModel userModel = servant.modelFactoryService.toUserModel(reviewPoint.getUser());
        ReviewPoint reviewPointForAnswer = userModel.getReviewPointFor(getAnswerLink());
        if (reviewPointForAnswer != null) result.add(reviewPointForAnswer);
        link.categoryLink().ifPresent(l -> {
            ReviewPoint reviewPointFor = userModel.getReviewPointFor(l);
            if (reviewPointFor != null) {
                result.add(reviewPointFor);
            }
        });
        return result;
    }

    @Override
    public List<Note> generateFillingOptions() {
        if (cachedFillingOptions == null) {
            cachedFillingOptions = link.getRemoteCousinOfDifferentCategory(reviewPoint.getUser())
                    .map(links ->
                            servant.randomizer.randomlyChoose(5, links).stream()
                                    .map(Link::getSourceNote).collect(Collectors.toList())
                    ).orElse(Collections.emptyList());
        }
        return cachedFillingOptions;
    }

}