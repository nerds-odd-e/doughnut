package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.json.LinkViewed;
import org.apache.logging.log4j.util.Strings;

import java.util.List;
import java.util.Map;

public class PictureSelectionQuizFactory implements QuizQuestionFactory {
    private final Note answerNote;

    public PictureSelectionQuizFactory(ReviewPoint reviewPoint) {
        this.answerNote = reviewPoint.getNote();
    }

    @Override
    public List<Note> generateFillingOptions(QuizQuestionServant servant) {
        return servant.choose5FromCohort(answerNote, n -> n.getNoteAccessories().hasPicture() && !n.equals(answerNote));
    }

    @Override
    public String generateInstruction() {
        return "";
    }

    @Override
    public String generateMainTopic() {
        return answerNote.getTitle();
    }

    @Override
    public Note generateAnswerNote(QuizQuestionServant servant) {
        return answerNote;
    }

    @Override
    public Map<Link.LinkType, LinkViewed> generateHintLinks() {
        return null;
    }

    @Override
    public QuizQuestion.OptionCreator optionCreator() {
        return new QuizQuestion.PictureOptionCreator();
    }

    @Override
    public boolean isValidQuestion() {
        return !Strings.isEmpty(answerNote.getNotePicture().orElse(null));
    }

    @Override
    public int minimumFillingOptionCount() {
        return 1;
    }

    @Override
    public List<Note> knownRightAnswers() {
        return List.of(answerNote);
    }

}