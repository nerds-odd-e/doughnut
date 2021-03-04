package com.odde.doughnut.services;

import com.odde.doughnut.entities.NoteMotionEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.entities.repositories.BazaarNoteRepository;
import com.odde.doughnut.entities.repositories.ReviewPointRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.models.NoteContentModel;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.models.NoteMotionModel;
import com.odde.doughnut.models.TreeNodeModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ModelFactoryService {
    @Autowired public final NoteRepository noteRepository;
    @Autowired public final UserRepository userRepository;
    @Autowired public final BazaarNoteRepository bazaarNoteRepository;
    @Autowired public final ReviewPointRepository reviewPointRepository;

    public ModelFactoryService(NoteRepository noteRepository, UserRepository userRepository, BazaarNoteRepository bazaarNoteRepository, ReviewPointRepository reviewPointRepository)
    {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.bazaarNoteRepository = bazaarNoteRepository;
        this.reviewPointRepository = reviewPointRepository;
    }

    public NoteContentModel toNoteModel(NoteEntity note) {
        return new NoteContentModel(note, this);
    }

    public TreeNodeModel toTreeNodeModel(NoteEntity note) {
        return new TreeNodeModel(note, this);
    }

    public NoteMotionModel toNoteMotionModel(NoteMotionEntity noteMotionEntity, NoteEntity noteEntity) {
        return new NoteMotionModel(noteMotionEntity, noteEntity, this);
    }

    public BazaarModel toBazaarModel() {
        return new BazaarModel(this);
    }

    public Optional<UserEntity> findUserById(Integer id) {
        return userRepository.findById(id);
    }

    public Optional<NoteEntity> findNoteById(Integer noteId) {
        return noteRepository.findById(noteId);
    }

}
