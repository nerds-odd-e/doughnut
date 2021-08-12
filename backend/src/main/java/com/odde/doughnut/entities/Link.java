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
        RELATED_TO                      (1, "related note", "related to", "is not related to", "is related to", new QuestionType[]{}),
        SPECIALIZE                      (2, "specification", "a specialization of", "is not a specialization of", "is a generalization of", new QuestionType[]{LINK_TARGET, LINK_SOURCE, WHICH_SPEC_HAS_INSTANCE, FROM_SAME_PART_AS, FROM_DIFFERENT_PART_AS, DESCRIPTION_LINK_TARGET}),
        APPLICATION                     (3, "application", "an application of", "is not an application of", "is a summary of", new QuestionType[]{LINK_TARGET, LINK_SOURCE, WHICH_SPEC_HAS_INSTANCE, FROM_SAME_PART_AS, FROM_DIFFERENT_PART_AS, DESCRIPTION_LINK_TARGET}),

        INSTANCE                        (4, "instance", "an instance of", "is not an instance of", "has instances", new QuestionType[]{LINK_TARGET, LINK_SOURCE, WHICH_SPEC_HAS_INSTANCE, FROM_SAME_PART_AS, FROM_DIFFERENT_PART_AS, DESCRIPTION_LINK_TARGET}),
        /*INTEGRATED*/ PART             (6, "part", "a part of", "is not a part of", "has parts", new QuestionType[]{LINK_TARGET, LINK_SOURCE, LINK_SOURCE_EXCLUSIVE, WHICH_SPEC_HAS_INSTANCE, FROM_SAME_PART_AS, FROM_DIFFERENT_PART_AS, DESCRIPTION_LINK_TARGET}),
        /*NON INTEGRATED*/ TAGGED_BY    (8, "tag target", "tagged by", "is not tagged by", "tagging", new QuestionType[]{LINK_TARGET, LINK_SOURCE, LINK_SOURCE_EXCLUSIVE, WHICH_SPEC_HAS_INSTANCE, WHICH_SPEC_HAS_INSTANCE, DESCRIPTION_LINK_TARGET}),
        ATTRIBUTE                       (10, "attribute", "an attribute of", "is not an attribute of", "has attributes", new QuestionType[]{LINK_TARGET, LINK_SOURCE, WHICH_SPEC_HAS_INSTANCE, WHICH_SPEC_HAS_INSTANCE, DESCRIPTION_LINK_TARGET }),

        OPPOSITE_OF                     (12, "opposition", "the opposite of", "is not the opposite of", "is the opposite", new QuestionType[]{LINK_TARGET, LINK_SOURCE, DESCRIPTION_LINK_TARGET}),
        AUTHOR_OF                       (14, "author", "author of", "is not author of", "is brought by", new QuestionType[]{LINK_TARGET, LINK_SOURCE, LINK_SOURCE_EXCLUSIVE, DESCRIPTION_LINK_TARGET}),
        USES                            (15, "user", "using", "is not using", "is used by", new QuestionType[]{LINK_TARGET, LINK_SOURCE, LINK_SOURCE_EXCLUSIVE, WHICH_SPEC_HAS_INSTANCE, FROM_SAME_PART_AS, FROM_DIFFERENT_PART_AS, DESCRIPTION_LINK_TARGET}),
        EXAMPLE_OF                      (17, "example", "an example of", "is not an example of", "has as examples", new QuestionType[]{LINK_SOURCE, LINK_SOURCE_EXCLUSIVE, CLOZE_LINK_TARGET, FROM_SAME_PART_AS, FROM_DIFFERENT_PART_AS}),
        PRECEDES                        (19, "precedence", "before", "is not before", "is after", new QuestionType[]{LINK_TARGET, LINK_SOURCE, LINK_SOURCE_EXCLUSIVE, DESCRIPTION_LINK_TARGET}),
        SIMILAR_TO                      (22, "thing", "similar to", "is not similar to", "is similar to", new QuestionType[]{LINK_TARGET, LINK_SOURCE, DESCRIPTION_LINK_TARGET}),
        CONFUSE_WITH                    (23, "thing", "confused with", "is not confused with", "is confused with", new QuestionType[]{});

        @JsonValue
        public final String label;
        public final String nameOfSource;
        public final Integer id;
        public final String exclusiveQuestion;
        public String reversedLabel;
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
    public List<Note> getCousinOfSameLinkType(User viewer) {
        return getCousinLinksOfSameLinkType(viewer).stream().map(Link::getSourceNote).collect(Collectors.toUnmodifiableList());
    }

    @JsonIgnore
    public List<Link> getCousinLinksOfSameLinkType(User viewer) {
        return targetNote.linksOfTypeThroughReverse(getLinkType(), viewer).filter(l->!l.equals(this)).collect(Collectors.toList());
    }

    @JsonIgnore
    public List<Note> getPiblingOfTheSameLinkType(User viewer) {
        return getPiblingLinksOfSameLinkType(viewer).stream().map(Link::getTargetNote).collect(Collectors.toUnmodifiableList());
    }

    @JsonIgnore
    public List<Link> getPiblingLinksOfSameLinkType(User viewer) {
        return sourceNote.linksOfTypeThroughDirect(List.of(getLinkType()), viewer).filter(l->!l.equals(this)).collect(Collectors.toList());
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

    @JsonIgnore
    public boolean sourceVisibleAsTargetOrTo(User viewer) {
        if (sourceNote.getNotebook() == targetNote.getNotebook()) return true;
        if (viewer == null) return false;

        return viewer.canReferTo(sourceNote.getNotebook());
    }

    @JsonIgnore
    public boolean targetVisibleAsSourceOrTo(User viewer) {
        if (sourceNote.getNotebook() == targetNote.getNotebook()) return true;
        if (viewer == null) return false;

        return viewer.canReferTo(targetNote.getNotebook());
    }

    @JsonIgnore
    public Stream<Link> categoryLinksOfTarget(User viewer) {
        return getTargetNote().linksOfTypeThroughDirect(List.of(LinkType.PART, LinkType.INSTANCE, LinkType.SPECIALIZE, LinkType.APPLICATION), viewer);
    }

    @JsonIgnore
    public List<Link> getReverseLinksOfCousins(User user, LinkType linkType) {
        return getCousinLinksOfSameLinkType(user).stream()
                .flatMap(p -> p.getSourceNote().linksOfTypeThroughReverse(linkType, user))
                .collect(Collectors.toList());
    }

}
