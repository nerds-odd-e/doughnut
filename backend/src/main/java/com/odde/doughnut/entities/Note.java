package com.odde.doughnut.entities;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.algorithms.SiblingOrder;

import org.hibernate.annotations.WhereJoinTable;
import org.springframework.beans.BeanUtils;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "note")
public class Note {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Embedded
    @Valid
    @Getter
    private final NoteContent noteContent = new NoteContent();

    @Column(name = "sibling_order")
    private Long siblingOrder = SiblingOrder.getGoodEnoughOrderNumber();

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "notebook_id", referencedColumnName = "id")
    @JsonIgnore
    @Getter
    private Notebook notebook;

    @Column(name = "created_at")
    @Getter
    @Setter
    private Timestamp createdAt;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "master_review_setting_id", referencedColumnName = "id")
    @JsonIgnore
    @Getter
    @Setter
    private ReviewSetting masterReviewSetting;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    @Getter
    @Setter
    private User user;

    @OneToMany(mappedBy = "sourceNote", cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    @Getter
    @Setter
    private List<Link> links = new ArrayList<>();

    @OneToMany(mappedBy = "targetNote", cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    @Getter
    @Setter
    private List<Link> refers = new ArrayList<>();

    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL)
    @JsonIgnore
    @OrderBy("depth DESC")
    @Getter
    @Setter
    private List<NotesClosure> notesClosures = new ArrayList<>();

    @OneToMany(mappedBy = "ancestor", cascade = CascadeType.DETACH)
    @JsonIgnore
    @OrderBy("depth")
    @Getter
    @Setter
    private List<NotesClosure> descendantNCs = new ArrayList<>();

    @JoinTable(name = "notes_closure", joinColumns = {
            @JoinColumn(name = "ancestor_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)}, inverseJoinColumns = {
            @JoinColumn(name = "note_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
    })
    @OneToMany(cascade = CascadeType.DETACH)
    @JsonIgnore
    @WhereJoinTable(clause = "depth = 1")
    @OrderBy("sibling_order")
    @Getter
    private final List<Note> children = new ArrayList<>();

    public static Note createNote(User user, NoteContent noteContent, Timestamp currentUTCTimestamp) {
        final Note note = new Note();
        try {
            note.updateNoteContent(noteContent, user);
        } catch (IOException e) {
            throw new RuntimeException(e.getLocalizedMessage());
        }
        note.setCreatedAtAndUpdatedAt(currentUTCTimestamp);
        note.setUser(user);
        return note;
    }

    @Override
    public String toString() {
        return "Note{" + "id=" + id + ", title='" + noteContent.getTitle() + '\'' + '}';
    }

    public String getShortDescription() {
        return noteContent.getShortDescription();
    }

    public String getShortDescriptionIDN() {
        return noteContent.getShortDescriptionIDN();
    }

    public String getNotePicture() {
        if (noteContent.getUseParentPicture() && getParentNote() != null) {
            return getParentNote().getNotePicture();
        }
        return noteContent.getNotePicture();
    }

    private void addAncestors(List<Note> ancestors) {
        int[] counter = {1};
        ancestors.forEach(anc -> {
            NotesClosure notesClosure = new NotesClosure();
            notesClosure.setNote(this);
            notesClosure.setAncestor(anc);
            notesClosure.setDepth(counter[0]);
            getNotesClosures().add(0, notesClosure);
            counter[0] += 1;
        });
    }

    public void setParentNote(Note parentNote) {
        if (parentNote == null) return;
        notebook = parentNote.getNotebook();
        List<Note> ancestors = parentNote.getAncestors();
        ancestors.add(parentNote);
        Collections.reverse(ancestors);
        addAncestors(ancestors);
    }

    @JsonIgnore
    public List<Note> getAncestors() {
        return getNotesClosures().stream().map(NotesClosure::getAncestor).collect(toList());
    }

    public void traverseBreadthFirst(Consumer<Note> noteConsumer) {
        descendantNCs.stream().map(NotesClosure::getNote).forEach(noteConsumer);
    }

    @JsonIgnore
    public Note getParentNote() {
        List<Note> ancestors = getAncestors();
        if (ancestors.size() == 0) {
            return null;
        }
        return ancestors.get(ancestors.size() - 1);
    }

    @JsonIgnore
    public List<Note> getSiblings() {
        if (getParentNote() == null) {
            return new ArrayList<>();
        }
        return getParentNote().getChildren();
    }

    public String getTitle() {
        return noteContent.getTitle();
    }

    public String getTitleIDN() {
        return noteContent.getTitleIDN();
    }

    public void mergeMasterReviewSetting(ReviewSetting reviewSetting) {
        ReviewSetting current = getMasterReviewSetting();
        if (current == null) {
            setMasterReviewSetting(reviewSetting);
        } else {
            BeanUtils.copyProperties(reviewSetting, getMasterReviewSetting());
        }
    }

    public void updateNoteContent(NoteContent noteContent, User user) throws IOException {
        noteContent.fetchUploadedPicture(user);

        boolean isTranslationOutdated = getNoteContent().getTitleIDN() != null;

        noteContent.setIsTranslationOutdatedIDN(isTranslationOutdated);

        mergeNoteContent(noteContent);
    }

    public void mergeNoteContent(NoteContent noteContent) {
        if (noteContent.getUploadPicture() == null) {
            noteContent.setUploadPicture(getNoteContent().getUploadPicture());
        }
        BeanUtils.copyProperties(noteContent, getNoteContent());
    }

    @JsonIgnore
    private Note getFirstChild() {
        return getChildren().stream().findFirst().orElse(null);
    }

    public void updateSiblingOrder(Note relativeToNote, boolean asFirstChildOfNote) {
        Long newSiblingOrder = relativeToNote.theSiblingOrderItTakesToMoveRelativeToMe(asFirstChildOfNote);
        if (newSiblingOrder != null) {
            siblingOrder = newSiblingOrder;
        }
    }

    private Optional<Note> nextSibling() {
        return getSiblings().stream()
                .filter(nc -> nc.siblingOrder > siblingOrder)
                .findFirst();
    }

    private long getSiblingOrderToInsertBehindMe() {
        Optional<Note> nextSiblingNote = nextSibling();
        return nextSiblingNote.map(x -> (siblingOrder + x.siblingOrder) / 2)
                .orElse(siblingOrder + SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT);
    }

    private Long getSiblingOrderToBecomeMyFirstChild() {
        Note firstChild = getFirstChild();
        if (firstChild != null) {
            return firstChild.siblingOrder - SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT;
        }
        return null;
    }

    private Long theSiblingOrderItTakesToMoveRelativeToMe(boolean asFirstChildOfNote) {
        if (!asFirstChildOfNote) {
            return getSiblingOrderToInsertBehindMe();
        }
        return getSiblingOrderToBecomeMyFirstChild();
    }

    public void buildNotebookForHeadNote(Ownership ownership, User creator) {
        final Notebook notebook = new Notebook();
        notebook.setCreatorEntity(creator);
        notebook.setOwnership(ownership);
        notebook.setHeadNote(this);

        this.notebook = notebook;
    }

    public Integer getParentId() {
        Note parent = getParentNote();
        if (parent == null) return null;
        return parent.id;
    }

    @JsonIgnore
    public Note getGrandAsPossible() {
        Note grand = this;
        for (int i = 0; i < 2; i++)
            if (grand.getParentNote() != null)
                grand = grand.getParentNote();
        return grand;
    }

    public void setCreatedAtAndUpdatedAt(Timestamp currentUTCTimestamp) {
        this.createdAt = currentUTCTimestamp;
        this.getNoteContent().setUpdatedAt(currentUTCTimestamp);
    }

    public Stream<Note> extractChildNotes(User user, Timestamp currentUTCTimestamp) {
        List<String> items = Arrays.asList(getNoteContent().getDescription().split("\n\n"));
        return items.stream()
                .filter(item -> !item.isBlank())
                .map(paragraph -> {
                            Note childNote = createNoteFromParagraph(paragraph, user, currentUTCTimestamp);
                            childNote.setParentNote(this);
                            return childNote;
                        }
                );
    }

    private Note createNoteFromParagraph(String paragraph, User user, Timestamp currentUTCTimestamp) {
        LinkedList<String> linesInParagraph = new LinkedList<>(Arrays.stream(paragraph.split("\n")).collect(toList()));

        NoteContent childNoteContent = new NoteContent();
        childNoteContent.setTitle(linesInParagraph.getFirst());
        linesInParagraph.removeFirst();
        childNoteContent.setDescription(String.join("\n", linesInParagraph));

        return Note.createNote(user, childNoteContent, currentUTCTimestamp);
    }
}

