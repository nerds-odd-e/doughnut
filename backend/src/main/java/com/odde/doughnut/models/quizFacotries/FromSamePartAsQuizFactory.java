package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinkViewed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FromSamePartAsQuizFactory implements QuizQuestionFactory {
    private Link cachedAnswerLink = null;
    private List<Note> cachedFillingOptions = null;
    private final QuizQuestionServant servant;
    private final ReviewPoint reviewPoint;
    private final Link link;

    public FromSamePartAsQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        this.servant = servant;
        this.reviewPoint = reviewPoint;
        this.link = reviewPoint.getLink();
    }

    @Override
    public List<Note> generateFillingOptions() {
        if (cachedFillingOptions == null) {
            cachedFillingOptions = new ArrayList<>();
            Link otherPart = this.link.getTargetNote().linksOfTypeThroughDirect(Link.LinkType.PART).findFirst().map(l -> {
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

    @Override
    public String generateInstruction() {
        return "<p>Which one " + link.getLinkTypeLabel() + " <mark>"+link.getTargetNote().getTitle()+"</mark> <em>and</em> " + getAnswerLink().getLinkTypeLabel() + " <mark>" + getAnswerLink().getTargetNote().getTitle() + "</mark>:" ;
    }

    @Override
    public String generateMainTopic() {
        return null;
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
        return generateAnswerNote() != null; //generateFillingOptions().size() > 0;
    }

    @Override
    public ReviewPoint getViceReviewPoint() {
        return null;
    }

    private Link getAnswerLink() {
        if(cachedAnswerLink == null) {
            List<Link> backwardPeers = link.getCousinLinks(reviewPoint.getUser());
            backwardPeers.remove(link);
            cachedAnswerLink = servant.randomizer.chooseOneRandomly(backwardPeers);
        }
        return cachedAnswerLink;
    }

}