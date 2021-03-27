package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.QuizQuestion;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.List;

public class PictureSelectTitleQuizFactory implements QuizQuestionFactory {
    private final NoteEntity answerNote;
    private final Randomizer randomizer;
    private final QuizQuestionServant servant;

    public PictureSelectTitleQuizFactory(ReviewPointEntity reviewPointEntity, Randomizer randomizer, ModelFactoryService modelFactoryService) {
        this.randomizer = randomizer;
        servant = new QuizQuestionServant(randomizer, modelFactoryService);
        this.answerNote = reviewPointEntity.getNoteEntity();
    }

    @Override
    public List<NoteEntity> generateFillingOptions() {
        return servant.choose5FromSiblings(answerNote, randomizer, n -> n.hasPicture() && !n.equals(answerNote));
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
    public NoteEntity generateAnswerNote() {
        return answerNote;
    }

    @Override
    public List<QuizQuestion.Option> toQuestionOptions(List<NoteEntity> noteEntities) {
        return servant.toPictureOptions(noteEntities);
    }

}