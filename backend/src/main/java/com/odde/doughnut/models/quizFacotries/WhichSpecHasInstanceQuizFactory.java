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
import java.util.stream.Stream;

public class WhichSpecHasInstanceQuizFactory implements QuizQuestionFactory {
    private Link cachedInstanceLink = null;
    private List<Note> cachedFillingOptions = null;
    private final QuizQuestionServant servant;
    private final ReviewPoint reviewPoint;
    private final Link link;

    public WhichSpecHasInstanceQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        this.servant = servant;
        this.reviewPoint = reviewPoint;
        this.link = reviewPoint.getLink();
    }

    @Override
    public List<Note> generateFillingOptions() {
        if (cachedFillingOptions != null) {
            return cachedFillingOptions;
        }
        List<Note> instanceReverse = getInstanceLink().getBackwardPeers();
        List<Note> specReverse = link.getBackwardPeers();
        List<Note> backwardPeers = Stream.concat(instanceReverse.stream(), specReverse.stream())
                .filter(n-> !(instanceReverse.contains(n) && specReverse.contains(n))).collect(Collectors.toList());
        cachedFillingOptions = servant.randomizer.randomlyChoose(5, backwardPeers);
        return cachedFillingOptions;
    }

    @Override
    public String generateInstruction() {
        return "<p>Which one " + link.getLinkTypeLabel() + " <mark>"+link.getTargetNote().getTitle()+"</mark> <em>and</em> " + getInstanceLink().getLinkTypeLabel() + " <mark>" + getInstanceLink().getTargetNote().getTitle() + "</mark>:" ;
    }

    @Override
    public String generateMainTopic() {
        return null;
    }

    @Override
    public Note generateAnswerNote() {
        if(getInstanceLink() == null) return null;
        return getInstanceLink().getSourceNote();
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
        return getViceReviewPoint() != null && generateFillingOptions().size() > 0;
    }

    @Override
    public ReviewPoint getViceReviewPoint() {
        if(getInstanceLink() == null) return null;
        UserModel userModel = servant.modelFactoryService.toUserModel(reviewPoint.getUser());
        return userModel.getReviewPointFor(getInstanceLink());
    }

    private Link getInstanceLink() {
        if(cachedInstanceLink == null) {
            List<Link> candidates = new ArrayList<>();
            candidates.add(servant.randomizer.chooseOneRandomly(link.getSourceNote().linksOfTypeThroughDirect(Link.LinkType.SPECIALIZE)));
            candidates.add(servant.randomizer.chooseOneRandomly(link.getSourceNote().linksOfTypeThroughDirect(Link.LinkType.INSTANCE)));
            candidates.add(servant.randomizer.chooseOneRandomly(link.getSourceNote().linksOfTypeThroughDirect(Link.LinkType.TAGGED_BY)));
            candidates.add(servant.randomizer.chooseOneRandomly(link.getSourceNote().linksOfTypeThroughDirect(Link.LinkType.ATTRIBUTE)));
            candidates.add(servant.randomizer.chooseOneRandomly(link.getSourceNote().linksOfTypeThroughDirect(Link.LinkType.USES)));
            candidates.add(servant.randomizer.chooseOneRandomly(link.getSourceNote().linksOfTypeThroughDirect(Link.LinkType.RELATED_TO)));
            cachedInstanceLink = servant.randomizer.chooseOneRandomly(candidates.stream().filter(l->{
                if(l==null) return false;
                return !link.equals(l);
            }).collect(Collectors.toList()));
        }
        return cachedInstanceLink;
    }

}