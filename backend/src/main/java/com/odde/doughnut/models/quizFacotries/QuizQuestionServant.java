package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NotesClosure;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.Randomizer;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class QuizQuestionServant {
    final Randomizer randomizer;
    final ModelFactoryService modelFactoryService;

    public QuizQuestionServant(Randomizer randomizer, ModelFactoryService modelFactoryService) {
        this.randomizer = randomizer;
        this.modelFactoryService = modelFactoryService;
    }

    List<Note> choose5FromCohort(Note answerNote, Predicate<Note> notePredicate) {
        List<Note> list = getCohort(answerNote, notePredicate);
        return randomizer.randomlyChoose(5, list);
    }

    private List<Note> getCohort(Note answerNote, Predicate<Note> notePredicate) {
        List<Note> list = answerNote.getSiblings().stream().filter(notePredicate).collect(Collectors.toList());
        if (list.size() > 1) return list;

        return answerNote.getGrandAsPossilbe().getDescendantNCs().stream().map(NotesClosure::getNote).filter(notePredicate).collect(Collectors.toList());
    }

    List<Note> randomlyChooseAndEnsure(List<Note> candidates, Note ensure, int maxSize) {
        List<Note> list = candidates.stream()
                .filter(n -> !n.equals(ensure)).collect(Collectors.toList());
        List<Note> selectedList = this.randomizer.randomlyChoose(maxSize - 1, list);
        selectedList.add(ensure);
        return selectedList;
    }

    Optional<Link> chooseOneCategoryLink(User user, Link link) {
        Link result = randomizer.chooseOneRandomly(
                link.categoryLinksOfTarget(user).collect(Collectors.toList()));
        return Optional.ofNullable(result);
    }
}