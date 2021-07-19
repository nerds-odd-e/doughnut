package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.json.LinkViewed;

import java.util.List;
import java.util.Map;

public class LinkTargetExclusiveQuizFactory implements QuizQuestionFactory {
    private final Link link;
    private final QuizQuestionServant servant;
    private List<Note> cachedFillingOptions = null;

    public LinkTargetExclusiveQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        this.link = reviewPoint.getLink();
        this.servant = servant;
    }

    @Override
    public List<Note> generateFillingOptions() {
        if(cachedFillingOptions == null) {
            Note sourceNote = link.getSourceNote();
            List<Note> backwardPeers = link.getBackwardPeers();
            cachedFillingOptions = servant.randomlyChooseAndEnsure(backwardPeers, sourceNote, 5);
        }
        return cachedFillingOptions;
    }

    @Override
    public String generateInstruction() {
        return String.format("Which of the following %s", link.getExclusiveQuestion());
    }

    @Override
    public String generateMainTopic() {
        return link.getTargetNote().getTitle();
    }

    @Override
    public Note generateAnswerNote() {
        Note note = link.getSourceNote();
        List<Note> siblings = note.getSiblings();
        siblings.removeAll(link.getBackwardPeers());
        siblings.remove(link.getTargetNote());
        return servant.randomizer.chooseOneRandomly(siblings);
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
        return generateFillingOptions().size() > 0;
    }
}