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
    private final ReviewPoint reviewPoint;
    private List<Note> cachedFillingOptions = null;
    private Note answerNote = null;

    public LinkTargetExclusiveQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        this.reviewPoint = reviewPoint;
        this.link = reviewPoint.getLink();
        this.servant = servant;
    }

    @Override
    public List<Note> generateFillingOptions(QuizQuestionServant servant) {
        if(cachedFillingOptions == null) {
            Note sourceNote = link.getSourceNote();
            List<Note> backwardPeers = link.getCousinOfSameLinkType(reviewPoint.getUser());
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
        if (answerNote == null) {
            Note note = link.getSourceNote();
            List<Note> siblings = note.getSiblings();
            siblings.removeAll(link.getCousinOfSameLinkType(reviewPoint.getUser()));
            siblings.remove(link.getTargetNote());
            siblings.remove(link.getSourceNote());
            answerNote = servant.randomizer.chooseOneRandomly(siblings);
        }
        return answerNote;
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
        return generateAnswerNote() !=null && generateFillingOptions(servant).size() > 0;
    }
}