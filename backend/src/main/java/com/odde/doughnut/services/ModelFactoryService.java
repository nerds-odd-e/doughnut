package com.odde.doughnut.services;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.*;
import com.odde.doughnut.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.validation.Valid;
import java.sql.Timestamp;
import java.util.Optional;

@Service
public class ModelFactoryService {
    @Autowired public final NoteRepository noteRepository;
    @Autowired public final UserRepository userRepository;
    @Autowired public final BazaarNoteRepository bazaarNoteRepository;
    @Autowired public final ReviewPointRepository reviewPointRepository;
    @Autowired public final CircleRepository circleRepository;
    @Autowired public final LinkRepository linkRepository;
    @Autowired public final EntityManager entityManager;

    public ModelFactoryService(NoteRepository noteRepository, UserRepository userRepository, BazaarNoteRepository bazaarNoteRepository, ReviewPointRepository reviewPointRepository, CircleRepository circleRepository, LinkRepository linkRepository, EntityManager entityManager)
    {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.bazaarNoteRepository = bazaarNoteRepository;
        this.reviewPointRepository = reviewPointRepository;
        this.circleRepository = circleRepository;
        this.linkRepository = linkRepository;
        this.entityManager = entityManager;
    }

    public NoteContentModel toNoteModel(NoteEntity note) {
        if(note == null) {
            return null;
        }
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

    public CircleModel toCircleModel(CircleEntity circleEntity) {
        return new CircleModel(circleEntity, this);
    }

    public CircleModel findCircleByInvitationCode(String invitationCode) {
        CircleEntity circleEntity = circleRepository.findFirstByInvitationCode(invitationCode);
        if (circleEntity == null) {
            return null;
        }
        return toCircleModel(circleEntity);
    }

    public AnswerModel toAnswerModel(AnswerEntity answerEntity) {
        return new AnswerModel(answerEntity, this);
    }
}
