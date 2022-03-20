package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.SearchTerm;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.apache.logging.log4j.util.Strings;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SearchTermModel {
    private final User user;
    private final ModelFactoryService modelFactoryService;
    private final SearchTerm searchTerm;

    public SearchTermModel(User entity, ModelFactoryService modelFactoryService, SearchTerm searchTerm) {
        this.user = entity;
        this.modelFactoryService = modelFactoryService;
        this.searchTerm = searchTerm;
    }

    private List<Note> search() {
        final String pattern = Pattern.quote(searchTerm.getTrimmedSearchKey());
        if (searchTerm.getAllMyCircles()) {
            return this.modelFactoryService.noteRepository.searchForUserInAllMyNotebooksSubscriptionsAndCircle(user, pattern);
        }
        if (searchTerm.getAllMyNotebooksAndSubscriptions()) {
            return this.modelFactoryService.noteRepository.searchForUserInAllMyNotebooksAndSubscriptions(user, pattern);
        }
        Notebook notebook = searchTerm.note.map(Note::getNotebook).orElse(null);
        return this.modelFactoryService.noteRepository.searchInNotebook(notebook, pattern);
    }

    public List<Note> searchForNotes() {
        if (Strings.isBlank(searchTerm.getTrimmedSearchKey())) {
            return null;
        }

        Integer avoidNoteId = searchTerm.note.map(Note::getId).orElse(null);
        return search().stream().filter(n -> !n.getId().equals(avoidNoteId)).collect(Collectors.toList());
    }
}
