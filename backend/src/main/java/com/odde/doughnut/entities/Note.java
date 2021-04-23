package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.algorithms.SiblingOrder;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
import org.hibernate.annotations.WhereJoinTable;
import org.springframework.beans.BeanUtils;

import javax.persistence.*;
import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Entity
@Table(name = "note")
public class Note {
    public BlogArticle toBlogArticle() {
        BlogArticle article = new BlogArticle();
        article.setTitle(getTitle());
        article.setDescription(getNoteContent().getDescription());
        article.setAuthor(getUser().getName());
        article.setCreatedDatetime(getArticleDate());
        return article;
    }

    public String getArticleDate() {
        var dateNote = getParentNote();
        var monthNote = dateNote.getParentNote();
        var yearNote = monthNote.getParentNote();
        return String.format("%s %s %s", dateNote.getTitle(), monthNote.getTitle(), yearNote.getTitle());
    }

    public static class NoteApiResult {
        @Getter
        @Setter
        private String title;

        @Getter
        @Setter
        private String description;

        @Getter
        @Setter
        private String author;

        @Getter
        @Setter
        private String updateDatetime;
    }


    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Embedded
    @Valid
    @Getter
    private NoteContent noteContent = new NoteContent();

    @Column(name = "sibling_order")
    private Long siblingOrder = SiblingOrder.getGoodEnoughOrderNumber();

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "notebook_id", referencedColumnName = "id")
    @JsonIgnore
    @Getter
    private Notebook notebook;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "master_review_setting_id", referencedColumnName = "id")
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

    @JoinTable(name = "notes_closure", joinColumns = {
            @JoinColumn(name = "ancestor_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)}, inverseJoinColumns = {
            @JoinColumn(name = "note_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
    })
    @OneToMany(cascade = CascadeType.DETACH)
    @JsonIgnore
    @WhereJoinTable(clause = "depth = 4")
    @OrderBy("sibling_order")
    @Getter
    private final List<Note> greatGreatGrandChildren = new ArrayList<>();

    @Override
    public String toString() {
        return "Note{" + "id=" + id + ", title='" + noteContent.getTitle() + '\'' + '}';
    }

    public List<Note> getTargetNotes() {
        return links.stream().map(Link::getTargetNote).collect(toList());
    }

    public List<Link.LinkType> linkTypes(User viewer) {
        return Arrays.stream(Link.LinkType.values())
                .filter(t -> !linkedNotesOfType(t, viewer).isEmpty())
                .collect(Collectors.toUnmodifiableList());
    }

    public List<Link> linksOfTypeThroughDirect(Link.LinkType linkType) {
        return this.links.stream()
                .filter(l -> l.getLinkType().equals(linkType))
                .collect(Collectors.toList());
    }

    public List<Link> linksOfTypeThroughReverse(Link.LinkType linkType, User viewer) {
        return refers.stream()
                .filter(l -> l.getLinkType().equals(linkType.reverseType()))
                .filter(l -> l.sourceVisibleAsTargetOrTo(viewer))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<Note> linkedNotesOfType(Link.LinkType linkType, User viewer) {
        List<Note> notes = new ArrayList<>();
        linksOfTypeThroughDirect(linkType).forEach(lk -> notes.add(lk.getTargetNote()));
        linksOfTypeThroughReverse(linkType, viewer).forEach(lk -> notes.add(lk.getSourceNote()));
        return notes;
    }

    public String getNotePicture() {
        if (noteContent.getUseParentPicture() && getParentNote() != null) {
            return getParentNote().getNotePicture();
        }
        return noteContent.getNotePicture();
    }

    public boolean isHead() {
        return getParentNote() == null;
    }

    public String getNoteTypeDisplay(){
        return this.getNotebook().getNotebookType().getDisplay();
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
        List<Note> ancestorsIncludingMe = parentNote.getAncestorsIncludingMe();
        Collections.reverse(ancestorsIncludingMe);
        addAncestors(ancestorsIncludingMe);
    }

    public List<Note> getAncestorsIncludingMe() {
        List<Note> ancestors = getAncestors();
        ancestors.add(this);
        return ancestors;
    }

    public List<Note> getAncestors() {
        return getNotesClosures().stream().map(NotesClosure::getAncestor).collect(toList());
    }

    public void traverseBreadthFirst(Consumer<Note> noteConsumer) {
        descendantNCs.stream().map(NotesClosure::getNote).forEach(noteConsumer);
    }

    public Note getParentNote() {
        List<Note> ancestors = getAncestors();
        if (ancestors.size() == 0) {
            return null;
        }
        return ancestors.get(ancestors.size() - 1);
    }

    public List<Note> getSiblings() {
        if (getParentNote() == null) {
            return new ArrayList<>();
        }
        return getParentNote().getChildren();
    }

    public String getTitle() {
        return noteContent.getTitle();
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
        mergeNoteContent(noteContent);
    }

    public void mergeNoteContent(NoteContent noteContent) {
        if (noteContent.getUploadPicture() == null) {
            noteContent.setUploadPicture(getNoteContent().getUploadPicture());
        }
        BeanUtils.copyProperties(noteContent, getNoteContent());
    }

    public Note getPreviousSibling() {
        return getSiblings().stream()
                .filter(nc -> nc.siblingOrder < siblingOrder)
                .reduce((f, s) -> s).orElse(null);
    }

    public Note getNextSibling() {
        return getSiblings().stream()
                .filter(nc -> nc.siblingOrder > siblingOrder)
                .findFirst().orElse(null);
    }

    public Note getPrevious() {
        Note result = getPreviousSibling();
        if (result == null) {
            return getParentNote();
        }
        while (true) {
            List<Note> children = result.getChildren();
            if (children.size() == 0) {
                return result;
            }
            result = children.get(children.size() - 1);
        }
    }

    private Note getFirstChild() {
        return getChildren().stream().findFirst().orElse(null);
    }

    public Note getNext() {
        Note firstChild = getFirstChild();
        if (firstChild != null) {
            return firstChild;
        }
        Note next = this;
        while (next != null) {
            Note sibling = next.getNextSibling();
            if (sibling != null) {
                return sibling;
            }
            next = next.getParentNote();
        }
        return null;
    }

    public void updateSiblingOrder(Note relativeToNote, boolean asFirstChildOfNote) {
        Long newSiblingOrder = relativeToNote.theSiblingOrderItTakesToMoveRelativeToMe(asFirstChildOfNote);
        if (newSiblingOrder != null) {
            siblingOrder = newSiblingOrder;
        }
    }

    private long getSiblingOrderToInsertBehindMe() {
        Note nextSiblingNote = getNextSibling();
        Long relativeToSiblingOrder = siblingOrder;
        if (nextSiblingNote == null) {
            return relativeToSiblingOrder + SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT;
        }
        return (relativeToSiblingOrder + nextSiblingNote.siblingOrder) / 2;
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

    public boolean hasTitleInArticle() {
        if (hasNoDescriptionAndChild()) {
            return false;
        }
        return !noteContent.getHideTitleInArticle();
    }

    public String getArticleTitle() {
        if (hasNoDescriptionAndChild()) {
            return null;
        }
        return getTitle();
    }

    public String getArticleBody() {
        if (hasNoDescriptionAndChild()) {
            return getTitle();
        }
        return noteContent.getDescription();
    }

    private boolean hasNoDescriptionAndChild() {
        return Strings.isBlank(noteContent.getDescription()) && children.isEmpty();
    }

    public void buildNotebookForHeadNote(Ownership ownership, User creator) {
        final Notebook notebook = new Notebook();
        NotebookType notebookType = this.getNoteContent().getNotebookType();
        notebook.setNotebookType(notebookType);
        assignNotebookForHeadNote(notebook, ownership, creator);
    }

    public void assignNotebookForHeadNote(Notebook notebook, Ownership ownership, User creator) {
        notebook.setCreatorEntity(creator);
        notebook.setOwnership(ownership);
        notebook.setHeadNote(this);

        this.user = creator;
        this.notebook = notebook;
    }

    public Boolean isArticle(){
        return !isHead() && noteContent.getDescription() != null && !noteContent.getDescription().isEmpty();
    }
}

