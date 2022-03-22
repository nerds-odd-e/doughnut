package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.models.UserModel;

import java.util.*;
import java.util.stream.Collectors;

public class FromDifferentPartAsQuizPresenter implements QuizQuestionPresenter {
    protected final Link link;
    private Link categoryLink;

    public FromDifferentPartAsQuizPresenter(QuizQuestion quizQuestion) {
        this.link = quizQuestion.getReviewPoint().getLink();
        this.categoryLink = quizQuestion.getCategoryLink();
    }

    @Override
    public String generateInstruction() {
        return "<p>Which one <mark>is " + link.getLinkTypeLabel() + "</mark> a <em>DIFFERENT</em> "+categoryLink.getLinkType().nameOfSource+" <mark>" + categoryLink.getTargetNote().getTitle() + "</mark> than:";
    }
}