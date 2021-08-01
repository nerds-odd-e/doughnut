package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.models.UserModel;

import java.util.*;
import java.util.stream.Collectors;

public class FromSamePartAsQuizFactory implements QuizQuestionFactory {
    private Link cachedAnswerLink = null;
    private List<Note> cachedFillingOptions = null;
    protected final QuizQuestionServant servant;
    protected final ReviewPoint reviewPoint;
    protected final Link link;
    private Optional<Link> categoryLink = null;

    public FromSamePartAsQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        this.servant = servant;
        this.reviewPoint = reviewPoint;
        this.link = reviewPoint.getLink();
    }

    @Override
    public List<Note> generateFillingOptions(QuizQuestionServant servant) {
        if (cachedFillingOptions == null) {
            cachedFillingOptions = getCategoryLink()
                    .map(lk->
                    servant.randomizer.randomlyChoose(
                    5, link.getRemoteCousinOfDifferentCategory(lk, reviewPoint.getUser()))
                            .stream().map(Link::getSourceNote).collect(Collectors.toList())).orElse(Collections.emptyList());
        }
        return cachedFillingOptions;
    }

    @Override
    public String generateInstruction() {
        return "<p>Which one <mark>" +link.getLinkTypeLabel() +"</mark> the same <mark>" + getCategoryLink().map(lk->lk.getTargetNote().getTitle()).orElse("") + "</mark> as:";
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
    public List<QuizQuestion.Option> toQuestionOptions(QuizQuestionServant servant, List<Note> notes) {
        return servant.toTitleOptions(notes);
    }

    @Override
    public Map<Link.LinkType, LinkViewed> generateHintLinks() {
        return null;
    }

    @Override
    public boolean isValidQuestion() {
        return generateAnswerNote() != null && !getViceReviewPoints().isEmpty() && generateFillingOptions(this.servant).size() > 0;
    }

    @Override
    public List<ReviewPoint> getViceReviewPoints() {
        Link answerLink = getAnswerLink();
        if (answerLink != null) {
            UserModel userModel = servant.modelFactoryService.toUserModel(reviewPoint.getUser());
            ReviewPoint answerLinkReviewPoint = userModel.getReviewPointFor(cachedAnswerLink);
            List<ReviewPoint> result = new ArrayList<>();
            result.add(answerLinkReviewPoint);
            getCategoryLink().map(userModel::getReviewPointFor).ifPresent(result::add);
            return result;
        }
        return Collections.emptyList();
    }

    private Optional<Link> getCategoryLink() {
        if(categoryLink == null) {
            categoryLink = servant.chooseOneCategoryLink(reviewPoint.getUser(), link);
        }
        return categoryLink;
    }

    protected Link getAnswerLink() {
        if (cachedAnswerLink == null) {
            UserModel userModel = servant.modelFactoryService.toUserModel(reviewPoint.getUser());
            List<Link> backwardPeers = link.getCousinLinksOfSameLinkType(reviewPoint.getUser()).stream()
                    .filter(l->userModel.getReviewPointFor(l) != null).collect(Collectors.toUnmodifiableList());
            cachedAnswerLink = servant.randomizer.chooseOneRandomly(backwardPeers);
        }
        return cachedAnswerLink;
    }

}