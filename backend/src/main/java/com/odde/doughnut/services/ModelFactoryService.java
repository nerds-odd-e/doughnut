package com.odde.doughnut.services;

import com.odde.doughnut.entities.NoteMotionEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.entities.repositories.BazaarNoteRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.models.NoteModel;
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

    public ModelFactoryService(NoteRepository noteRepository, UserRepository userRepository, BazaarNoteRepository bazaarNoteRepository)
    {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.bazaarNoteRepository = bazaarNoteRepository;
    }

    public NoteModel toNoteModel(NoteEntity note) {
        return new NoteModel(note, this);
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

    public NoteMotionEntity getLeftNoteMotion(NoteEntity noteEntity) {
        TreeNodeModel treeNodeModel = toTreeNodeModel(noteEntity);
        NoteEntity previousSiblingNote = treeNodeModel.getPreviousSiblingNote();
        TreeNodeModel prev = toTreeNodeModel(previousSiblingNote);
        NoteEntity prevprev = prev.getPreviousSiblingNote();
        if (prevprev == null) {
            return new NoteMotionEntity(noteEntity.getParentNote(), true);
        }
        return new NoteMotionEntity(prevprev, false);
    }

    public NoteMotionEntity getRightNoteMotion(NoteEntity noteEntity) {
        TreeNodeModel treeNodeModel = toTreeNodeModel(noteEntity);
        return new NoteMotionEntity(treeNodeModel.getNextSiblingNote(), false);
    }

}
