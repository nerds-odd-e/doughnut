package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.UserModel;

import java.util.*;
import java.util.stream.Collectors;

public class FromDifferentPartAsQuizFactory implements QuizQuestionFactory {
    protected final ReviewPoint reviewPoint;
    protected final Link link;
    private List<Note> cachedFillingOptions = null;
    private Optional<Link> categoryLink = null;

    public FromDifferentPartAsQuizFactory(ReviewPoint reviewPoint) {
        this.reviewPoint = reviewPoint;
        this.link = reviewPoint.getLink();
    }

    @Override
    public int minimumFillingOptionCount() {
        return 1;
    }

    @Override
    public List<Note> allWrongAnswers() {
        List<Note> result = new ArrayList<>(reviewPoint.getLink().getCousinOfSameLinkType(reviewPoint.getUser()));
        result.add(link.getSourceNote());
        return result;
    }

    @Override
    public List<Note> generateFillingOptions(QuizQuestionServant servant) {
        if (cachedFillingOptions == null) {
            List<Link> cousinLinks = link.getCousinLinksOfSameLinkType(reviewPoint.getUser());
            cachedFillingOptions = servant.randomizer.randomlyChoose(5, cousinLinks).stream()
                    .map(Link::getSourceNote).collect(Collectors.toList());
        }
        return cachedFillingOptions;
    }

    @Override
    public String generateInstruction() {
        return "<p>Which one <mark>is " + link.getLinkTypeLabel() + "</mark> a <em>DIFFERENT</em> "+categoryLink.map(lk->lk.getLinkType().nameOfSource).orElse("")+" <mark>" + categoryLink.map(lk->lk.getTargetNote().getTitle()).orElse("") + "</mark> than:";
    }

    @Override
    public Link getCategoryLink() {
        return  this.categoryLink.orElse(null);
    }

    @Override
    public Note generateAnswerNote(QuizQuestionServant servant) {
        categoryLink = servant.chooseOneCategoryLink(reviewPoint.getUser(), link);
        return categoryLink
                .map(lk -> lk.getReverseLinksOfCousins(reviewPoint.getUser(), link.getLinkType()))
                .map(servant.randomizer::chooseOneRandomly)
                .map(Link::getSourceNote)
                .orElse(null);
    }

    @Override
    public List<ReviewPoint> getViceReviewPoints(UserModel userModel) {
        return categoryLink.map(userModel::getReviewPointFor).map(List::of).orElse(Collections.emptyList());
    }

}