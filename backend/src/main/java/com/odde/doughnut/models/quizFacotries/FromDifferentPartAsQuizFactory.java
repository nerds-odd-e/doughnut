package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.models.UserModel;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FromDifferentPartAsQuizFactory implements QuizQuestionFactory {
    protected final QuizQuestionServant servant;
    protected final ReviewPoint reviewPoint;
    protected final Link link;
    private Link cachedAnswerLink = null;
    private List<Note> cachedFillingOptions = null;
    private ReviewPoint categoryLinkAsViceReviewPoint = null;

    public FromDifferentPartAsQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        this.servant = servant;
        this.reviewPoint = reviewPoint;
        this.link = reviewPoint.getLink();
    }

    protected Link getAnswerLink() {
        if (cachedAnswerLink == null) {
            cachedAnswerLink = servant.randomizer.chooseOneRandomly(link.getRemoteCousinOfDifferentCategory(getCategoryLinkAsViceReviewPoint().getLink(), reviewPoint.getUser()));
        }
        return cachedAnswerLink;
    }

    @Override
    public boolean isValidQuestion() {
        return generateAnswerNote() != null && generateFillingOptions().size() > 0;
    }

    @Override
    public List<ReviewPoint> getViceReviewPoints() {
        ReviewPoint vrp = getCategoryLinkAsViceReviewPoint();
        if (vrp != null) {
            return List.of(vrp);
        }
        return Collections.emptyList();
    }

    private ReviewPoint getCategoryLinkAsViceReviewPoint() {
        if(categoryLinkAsViceReviewPoint == null) {
            UserModel userModel = servant.modelFactoryService.toUserModel(reviewPoint.getUser());
            categoryLinkAsViceReviewPoint = servant.randomizer.chooseOneRandomly(link.categoryLinks(userModel.getEntity()).map(userModel::getReviewPointFor).collect(Collectors.toList()));
        }
        return categoryLinkAsViceReviewPoint;
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
        return "<p>Which one <mark>" + link.getLinkTypeLabel()+"</mark> a <em>DIFFERENT</em> <mark>" + getCategoryLinkAsViceReviewPoint().getLink().getTargetNote().getTitle() + "</mark> than:";
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