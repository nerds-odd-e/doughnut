package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import org.apache.logging.log4j.util.Strings;

import java.util.List;

public class ClozeTitleSelectionQuizFactory extends ClozeDescriptonQuizFactory implements QuestionOptionsFactory {
    public ClozeTitleSelectionQuizFactory(ReviewPoint reviewPoint) {
        super(reviewPoint);
    }

    @Override
    public Note generateAnswerNote(QuizQuestionServant servant) {
        return answerNote;
    }

    @Override
    public List<Note> generateFillingOptions(QuizQuestionServant servant) {
        return servant.choose5FromCohort(answerNote, n -> !n.equals(answerNote));
    }

    @Override
    public boolean isValidQuestion() {
        return !Strings.isEmpty(reviewPoint.getNote().getTextContent().getDescription());
    }

    @Override
    public int minimumOptionCount() {
        return 1;
    }
}