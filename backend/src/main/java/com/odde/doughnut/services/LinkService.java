package com.odde.doughnut.services;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.repositories.NoteRepository;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class LinkService {
    private final NoteRepository noteRepository;
    public LinkService(NoteRepository noteRepository)
    {
        this.noteRepository = noteRepository;
    }

    public void linkNote(Integer sourceNoteId, Integer targetNoteId) {

        Note sourceNote = noteRepository.findById(sourceNoteId).get();
        Note targetNote = noteRepository.findById(targetNoteId).get();

        sourceNote.linkToNote(targetNote);
        sourceNote.setUpdatedDatetime(new Date());

        noteRepository.save(sourceNote);
    }
}
