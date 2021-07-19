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
        if(cachedFillingOptions == null) {
            Note sourceNote = link.getSourceNote();
            List<Note> backwardPeers = link.getBackwardPeers().stream().filter(n->!n.equals(sourceNote)).collect(Collectors.toList());
            cachedFillingOptions = servant.randomizer.randomlyChoose(5, backwardPeers);
        }
        return cachedFillingOptions;
    }

    @Override
    public String generateInstruction() {
        return "<p>Which one " + link.getLinkTypeLabel() + " <mark>"+link.getTargetNote().getTitle()+"</mark> <em>And</em> " + getInstanceLink().getLinkTypeLabel() + " <mark>" + getInstanceLink().getTargetNote().getTitle() + "</mark>:" ;
    }

    @Override
    public String generateMainTopic() {
        return null;
    }

    @Override
    public Note generateAnswerNote() {
        if(getInstanceLink() == null) return null;
        return getInstanceLink().getTargetNote();
    }

    @Override
    public List<QuizQuestion.Option> toQuestionOptions(List<Note> notes) {
        return null;
    }

    @Override
    public Map<Link.LinkType, LinkViewed> generateHintLinks() {
        return null;
    }

    @Override
    public boolean isValidQuestion() {
        return getInstanceLink() != null && generateFillingOptions().size() > 0;
    }

    private Link getInstanceLink() {
        if(cachedInstanceLink == null) {
            cachedInstanceLink = servant.randomizer.chooseOneRandomly(link.getSourceNote().linksOfTypeThroughDirect(Link.LinkType.INSTANCE));
        }
        return cachedInstanceLink;
    }

}