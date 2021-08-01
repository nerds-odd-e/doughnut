package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.models.UserModel;

import java.util.ArrayList;
import java.util.Collections;
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
    public List<Note> generateFillingOptions(QuizQuestionServant servant) {
        if (cachedFillingOptions != null) {
            return cachedFillingOptions;
        }
        List<Note> instanceReverse = getInstanceLink().getCousinOfSameLinkType(reviewPoint.getUser());
        List<Note> specReverse = link.getCousinOfSameLinkType(reviewPoint.getUser());
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
        return !getViceReviewPoints().isEmpty() && generateFillingOptions(this.servant).size() > 0;
    }

    @Override
    public List<ReviewPoint> getViceReviewPoints() {
        if(getInstanceLink() != null) {
            UserModel userModel = servant.modelFactoryService.toUserModel(reviewPoint.getUser());
            ReviewPoint reviewPointFor = userModel.getReviewPointFor(getInstanceLink());
            if (reviewPointFor != null) {
                return List.of(reviewPointFor);
            }
        }
        return Collections.emptyList();
    }

    private Link getInstanceLink() {
        if(cachedInstanceLink == null) {
            List<Link> candidates = new ArrayList<>();
            populateCandidate(candidates, Link.LinkType.SPECIALIZE);
            populateCandidate(candidates, Link.LinkType.APPLICATION);
            populateCandidate(candidates, Link.LinkType.INSTANCE);
            populateCandidate(candidates, Link.LinkType.TAGGED_BY);
            populateCandidate(candidates, Link.LinkType.ATTRIBUTE);
            populateCandidate(candidates, Link.LinkType.USES);
            populateCandidate(candidates, Link.LinkType.RELATED_TO);
            cachedInstanceLink = servant.randomizer.chooseOneRandomly(candidates.stream().filter(l->{
                if(l==null) return false;
                return !link.equals(l);
            }).collect(Collectors.toList()));
        }
        return cachedInstanceLink;
    }

    private void populateCandidate(List<Link> candidates, Link.LinkType specialize) {
        candidates.add(servant.randomizer.chooseOneRandomly(link.getSourceNote().linksOfTypeThroughDirect(specialize, reviewPoint.getUser()).collect(Collectors.toUnmodifiableList())));
    }

}