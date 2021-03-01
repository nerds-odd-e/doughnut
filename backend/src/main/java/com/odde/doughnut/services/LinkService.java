package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.models.NoteModel;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class LinkService {
    private final NoteRepository noteRepository;
    private final ModelFactoryService modelFactoryService;
    public LinkService(NoteRepository noteRepository, ModelFactoryService modelFactoryService)
    {
        this.noteRepository = noteRepository;
        this.modelFactoryService = modelFactoryService;
    }

    public void linkNote(Note sourceNote, Note targetNote) {
        NoteModel noteModel = modelFactoryService.toModel(sourceNote);
        noteModel.linkNote(targetNote);
    }

}
