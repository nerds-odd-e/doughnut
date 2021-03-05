package com.odde.doughnut.services;

import com.odde.doughnut.entities.NoteMotionEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.entities.repositories.BazaarNoteRepository;
import com.odde.doughnut.entities.repositories.ReviewPointRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.models.*;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.repositories.NoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.validation.Valid;
import java.util.Optional;

@Service
public class ModelFactoryService {
    @Autowired public final NoteRepository noteRepository;
    @Autowired public final UserRepository userRepository;
    @Autowired public final BazaarNoteRepository bazaarNoteRepository;
    @Autowired public final ReviewPointRepository reviewPointRepository;
    @Autowired public final EntityManager entityManager;

    public ModelFactoryService(NoteRepository noteRepository, UserRepository userRepository, BazaarNoteRepository bazaarNoteRepository, ReviewPointRepository reviewPointRepository, EntityManager entityManager)
    {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.bazaarNoteRepository = bazaarNoteRepository;
        this.reviewPointRepository = reviewPointRepository;
        this.entityManager = entityManager;
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

    public UserModel toUserModel(UserEntity userEntity) {
        if (userEntity == null) {
            return null;
        }
        return new UserModel(userEntity, this);
    }

    public ReviewPointModel toReviewPointModel(@Valid ReviewPointEntity reviewPointEntity) {
        return new ReviewPointModel(reviewPointEntity, this);
    }

    public <T, M extends ModelForEntity<T>> M toModel(T entity, Class<M> klass){
        if (klass == ReviewPointModel.class) {
            return (M) toReviewPointModel((ReviewPointEntity) entity);
        }
        if (klass == UserModel.class) {
            return (M) toUserModel((UserEntity) entity);
        }
        return null;
    }

}
