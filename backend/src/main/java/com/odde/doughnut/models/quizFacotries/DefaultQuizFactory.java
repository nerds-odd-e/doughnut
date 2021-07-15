package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.LinkViewed;
import org.apache.logging.log4j.util.Strings;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultQuizFactory implements QuizQuestionFactory {
    protected final ReviewPoint reviewPoint;
    protected final Note answerNote;
    protected final QuizQuestionServant servant;

    public DefaultQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        this.reviewPoint = reviewPoint;
        this.servant = servant;
        this.answerNote = getAnswerNote();
    }

    @Override
    public List<Note> generateFillingOptions() {
        return servant.choose5FromSiblings(answerNote, n -> !n.equals(answerNote));
    }

    @Override
    public String generateInstruction() {
        return answerNote.getNoteContent().getClozeDescription();
    }

    @Override
    public String generateMainTopic() {
        return "";
    }

    @Override
    public Note generateAnswerNote() {
        return answerNote;
    }

    @Override
    public List<QuizQuestion.Option> toQuestionOptions(List<Note> noteEntities) {
        return servant.toTitleOptions(noteEntities);
    }

    @Override
    public Map<Link.LinkType, LinkViewed> generateHintLinks() {
        return answerNote.getAllLinks(reviewPoint.getUser()).entrySet().stream()
                .filter(x -> Link.LinkType.openTypes().anyMatch((y)->x.getKey().equals(y)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public boolean isValidQuestion() {
        return !Strings.isEmpty(reviewPoint.getNote().getNoteContent().getDescription());
    }

    private Note getAnswerNote() {
        return reviewPoint.getNote();
    }

}