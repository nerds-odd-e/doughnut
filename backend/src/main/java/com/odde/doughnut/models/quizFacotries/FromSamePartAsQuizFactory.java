package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.models.UserModel;

import java.util.*;

public class FromSamePartAsQuizFactory implements QuizQuestionFactory {
    private Link cachedAnswerLink = null;
    private List<Note> cachedFillingOptions = null;
    private final QuizQuestionServant servant;
    private final ReviewPoint reviewPoint;
    private final Link link;
    private Optional<Link> cachedCategoryLink = null;
    private ReviewPoint cachedAnswerLinkReviewPoint = null;

    public FromSamePartAsQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        this.servant = servant;
        this.reviewPoint = reviewPoint;
        this.link = reviewPoint.getLink();
    }

    @Override
    public List<Note> generateFillingOptions() {
        if (cachedFillingOptions == null) {
            cachedFillingOptions = new ArrayList<>();
            Link otherPart = categoryLink().map(l -> {
                return servant.randomizer.chooseOneRandomly(l.getCousinLinks(reviewPoint.getUser()));
            }).orElse(null);
            if (otherPart != null) {
               otherPart.getSourceNote().linksOfTypeThroughReverse(link.getLinkType(), reviewPoint.getUser()).forEach(l->{
                   cachedFillingOptions.add(l.getSourceNote());
               });
            }
        }
        return cachedFillingOptions;
    }

    private Optional<Link> categoryLink() {
        if (cachedCategoryLink == null) {
            cachedCategoryLink = this.link.getTargetNote().linksOfTypeThroughDirect(Link.LinkType.PART).findFirst();
        }
        return cachedCategoryLink;
    }

    @Override
    public String generateInstruction() {
        return "<p>Which one's <mark>" + categoryLink().map(l->l.getTargetNote().getTitle()).orElse("") + "</mark> is the same as:";
    }

    @Override
    public String generateMainTopic() {
        return link.getSourceNote().getTitle();
    }

    @Override
    public Note generateAnswerNote() {
        if(getAnswerLink() == null) return null;
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

    @Override
    public boolean isValidQuestion() {
        return generateAnswerNote() != null && !getViceReviewPoints().isEmpty() && generateFillingOptions().size() > 0;
    }

    @Override
    public List<ReviewPoint> getViceReviewPoints() {
        getAnswerLink();
        if(cachedAnswerLinkReviewPoint != null) {
            List<ReviewPoint> result = new ArrayList<>();
            result.add(cachedAnswerLinkReviewPoint);
            UserModel userModel = servant.modelFactoryService.toUserModel(reviewPoint.getUser());
            categoryLink().ifPresent(l-> {
                ReviewPoint reviewPointFor = userModel.getReviewPointFor(l);
                if(reviewPointFor != null) {
                    result.add(reviewPointFor);
                }
            });
            return result;
        }
        return Collections.emptyList();
    }

    private Link getAnswerLink() {
        if(cachedAnswerLink == null) {
            List<Link> backwardPeers = link.getCousinLinks(reviewPoint.getUser());
            backwardPeers.remove(link);
            cachedAnswerLink = servant.randomizer.chooseOneRandomly(backwardPeers);
            if(cachedAnswerLink != null) {
                UserModel userModel = servant.modelFactoryService.toUserModel(reviewPoint.getUser());
                this.cachedAnswerLinkReviewPoint = userModel.getReviewPointFor(cachedAnswerLink);
            }
        }
        return cachedAnswerLink;
    }

}