package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.models.UserModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FromDifferentPartAsQuizFactory implements QuizQuestionFactory {
    protected final QuizQuestionServant servant;
    protected final ReviewPoint reviewPoint;
    protected final Link link;
    private Link cachedAnswerLink = null;
    private List<Note> cachedFillingOptions = null;

    public FromDifferentPartAsQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        this.servant = servant;
        this.reviewPoint = reviewPoint;
        this.link = reviewPoint.getLink();
    }

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
            List<Link> cousinLinks = link.getCousinLinks(reviewPoint.getUser());
            cachedFillingOptions = servant.randomizer.randomlyChoose(5, cousinLinks).stream()
                    .map(Link::getSourceNote).collect(Collectors.toList());
        }
        return cachedFillingOptions;
    }

    @Override
    public String generateInstruction() {
        return "<p>Which one <mark>" + link.getLinkTypeLabel()+"</mark> a <em>DIFFERENT</em> <mark>" + link.categoryLink().map(l -> l.getTargetNote().getTitle()).orElse("") + "</mark> than:";
    }

    @Override
    public String generateMainTopic() {
        return link.getSourceNote().getTitle();
    }

    @Override
    public Note generateAnswerNote() {
        if (getAnswerLink() == null) return null;
        return getAnswerLink().getSourceNote();
    }

    @Override
    public List<QuizQuestion.Option> toQuestionOptions(List<Note> notes) {
        return servant.toTitleOptions(notes);
    }

    @Override
    public Map<Link.LinkType, LinkViewed> generateHintLinks() {
        return null;
    }
}