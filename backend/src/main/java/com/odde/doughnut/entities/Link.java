package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;
import com.odde.doughnut.entities.QuizQuestion.QuestionType;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.*;

@Entity
@Table(name = "link")
public class Link {

    public enum LinkType {
        RELATED_TO                            (1, "related note", "is related to", "is not related to", "is related to", new QuestionType[]{}),
        SPECIALIZE                            (2, "specification", "is a specialization of", "is not a specialization of", "is a generalization of", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE, WHICH_SPEC_HAS_INSTANCE, FROM_SAME_PART_AS, FROM_DIFFERENT_PART_AS}),

        INSTANCE                              (4, "instance", "is an instance of", "is not an instance of", "", new QuestionType[]{LINK_TARGET, WHICH_SPEC_HAS_INSTANCE, FROM_SAME_PART_AS, FROM_DIFFERENT_PART_AS}),
        HAS_INSTANCE                          (5, "instance owner", "has instances", "not have as an instance", "", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        /*INTEGRATED*/ PART                   (6, "part", "is a part of", "is not a part of", "", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE, WHICH_SPEC_HAS_INSTANCE, FROM_SAME_PART_AS, FROM_DIFFERENT_PART_AS}),
        HAS_PART                              (7, "part owner", "has parts", "not have as a part", "", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        /*NON INTEGRATED*/ TAGGED_BY          (8, "tag target", "is tagged by", "is not tagged by", "", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE, WHICH_SPEC_HAS_INSTANCE, WHICH_SPEC_HAS_INSTANCE}),
        TAGGING                               (9, "tag", "tagging", "is not tagging", "", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        ATTRIBUTE                             (10, "attribute", "is an attribute of", "is not an attribute of", "", new QuestionType[]{LINK_TARGET, WHICH_SPEC_HAS_INSTANCE, WHICH_SPEC_HAS_INSTANCE }),
        HAS_ATTRIBUTE                         (11, "attribute owner", "has attributes", "not has as an attribute", "", new QuestionType[]{LINK_TARGET }),

        OPPOSITE_OF                           (12, "opposition", "is the opposite of", "is not the opposite of", "", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        BROUGHT_BY                            (13, "masterpiece", "is brought by", "is not brought by", "", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        AUTHOR_OF                             (14, "author", "is author of", "is not author of", "", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE, DESCRIPTION_LINK_TARGET}),
        USES                                  (15, "user", "uses", "does not use", "", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE, WHICH_SPEC_HAS_INSTANCE, FROM_SAME_PART_AS, FROM_DIFFERENT_PART_AS}),
        USED_BY                               (16, "used", "is used by", "is not used by", "", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        EXAMPLE_OF                            (17, "example", "is an example of", "is not an example of", "", new QuestionType[]{LINK_SOURCE_EXCLUSIVE, CLOZE_LINK_TARGET, FROM_SAME_PART_AS, FROM_DIFFERENT_PART_AS}),
        HAS_AS_EXAMPLE                        (18, "example owner", "has as example", "does not have as example", "", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        PRECEDES                              (19, "precedence", "precedes", "does not precede", "", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        SUCCEEDS                              (20, "succession", "succeeds", "does not succeeds", "", new QuestionType[]{LINK_TARGET, LINK_SOURCE_EXCLUSIVE}),
        SAME_AS                               (21, "thing", "is the same as", "is not the same as", "", new QuestionType[]{LINK_TARGET}),
        SIMILAR_TO                            (22, "thing", "is similar to", "is not similar to", "", new QuestionType[]{LINK_TARGET}),
        CONFUSE_WITH                          (23, "thing", "confuses with", "does not confuse with", "", new QuestionType[0]);

        @JsonValue
        public final String label;
        public final String nameOfSource;
        public final Integer id;
        public final String exclusiveQuestion;
        private String reversedLabel;
        @Getter
        private final QuestionType[] questionTypes;

        LinkType(Integer id, String nameOfSource, String label, String exclusiveQuestion, String reversedLabel, QuestionType[] questionTypes) {
            this.nameOfSource = nameOfSource;
            this.label = label;
            this.id = id;
            this.exclusiveQuestion = exclusiveQuestion;
            this.reversedLabel = reversedLabel;
            this.questionTypes = questionTypes;
        }

        private static final Map<Integer, LinkType> idMap = Collections.unmodifiableMap(Arrays.stream(values()).collect(Collectors.toMap(x->x.id, x->x)));

        public static Stream<LinkType> openTypes() {
            final Link.LinkType[] openTypes = {Link.LinkType.TAGGED_BY, Link.LinkType.INSTANCE, Link.LinkType.PART};
            return Arrays.stream(openTypes);
        }

        public static LinkType fromLabel(String text) {
            for (LinkType b : LinkType.values()) {
                if (b.label.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }

        public static LinkType fromId(Integer id) {
            return idMap.getOrDefault(id, null);
        }

        public LinkType reverseType() {
            if (this.equals(BROUGHT_BY)) return AUTHOR_OF;
            if (this.equals(AUTHOR_OF)) return BROUGHT_BY;
            if (this.equals(USES)) return USED_BY;
            if (this.equals(USED_BY)) return USES;
            if (this.equals(HAS_AS_EXAMPLE)) return EXAMPLE_OF;
            if (this.equals(EXAMPLE_OF)) return HAS_AS_EXAMPLE;
            if (this.equals(PRECEDES)) return SUCCEEDS;
            if (this.equals(SUCCEEDS)) return PRECEDES;
            if (this.equals(INSTANCE)) return HAS_INSTANCE;
            if (this.equals(HAS_INSTANCE)) return INSTANCE;
            if (this.equals(PART)) return HAS_PART;
            if (this.equals(HAS_PART)) return PART;
            if (this.equals(TAGGED_BY)) return TAGGING;
            if (this.equals(TAGGING)) return TAGGED_BY;
            if (this.equals(ATTRIBUTE)) return HAS_ATTRIBUTE;
            if (this.equals(HAS_ATTRIBUTE)) return ATTRIBUTE;
            return this;
        }
    }

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "source_id", referencedColumnName = "id")
    @JsonIgnoreProperties("noteContent")
    @Getter @Setter private Note sourceNote;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "target_id", referencedColumnName = "id")
    @JsonIgnoreProperties("noteContent")
    @Getter @Setter private Note targetNote;

    @Column(name = "type_id")
    @Getter
    @Setter
    @JsonIgnore
    private Integer typeId;

    @JsonIgnore
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @Getter @Setter private User user;

    @Column(name = "created_at")
    @Getter
    @Setter
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    @OneToMany(mappedBy = "link", cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnore
    private final List<ReviewPoint> reviewPointEntities = new ArrayList<>();

    @JsonIgnore
    public LinkType getLinkType() {
        return LinkType.fromId(typeId);
    }

    public String getLinkTypeLabel() {
        return getLinkType().label;
    }

    public String getLinkNameOfSource() {
        return getLinkType().nameOfSource;
    }

    public void setLinkType(LinkType linkType) {
        if (linkType == null) {
            typeId = null;
        }
        typeId = linkType.id;
    }

    @JsonIgnore
    public List<Note> getCousinOfSameLinkType() {
        return targetNote.linkedNotesOfType(getLinkType().reverseType(), null);
    }

    @JsonIgnore
    public List<Link> getCousinLinks(User viewer) {
        return targetNote.linksOfTypeThroughReverse(getLinkType(), viewer).filter(l->!l.equals(this)).collect(Collectors.toList());
    }

    @JsonIgnore
    public String getExclusiveQuestion() {
        return getLinkType().exclusiveQuestion.replace("not ", "<em>NOT</em> ");
    }

    @JsonIgnore
    public List<LinkType> getPossibleLinkTypes() {
        final List<LinkType> existingTypes = sourceNote.getLinks().stream().filter(l -> l.targetNote == targetNote).map(Link::getLinkType).collect(Collectors.toUnmodifiableList());
        return Arrays.stream(LinkType.values()).filter(lt->
                lt == getLinkType() || !existingTypes.contains(lt)).collect(Collectors.toList());
    }

    public boolean sourceVisibleAsTargetOrTo(User viewer) {
        if (sourceNote.getNotebook() == targetNote.getNotebook()) return true;
        if (viewer == null) return false;

        return viewer.canReferTo(sourceNote.getNotebook());
    }

    public Optional<Link> categoryLink() {
        return getTargetNote().linksOfTypeThroughDirect(LinkType.PART).findFirst();
    }

    public Optional<List<Link>> getRemoteCousinOfDifferentCategory(User user) {
        return categoryLink().map(l -> l.getCousinLinks(user))
                .map(otherParts -> otherParts.stream()
                        .flatMap(p -> p.getSourceNote().linksOfTypeThroughReverse(getLinkType(), user))
                        .collect(Collectors.toList()));
    }

}
