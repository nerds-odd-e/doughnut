package com.odde.doughnut.entities;

import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.CLOZE_LINK_TARGET;
import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.DESCRIPTION_LINK_TARGET;
import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.FROM_DIFFERENT_PART_AS;
import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.FROM_SAME_PART_AS;
import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.LINK_SOURCE;
import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.LINK_SOURCE_WITHIN_SAME_LINK_TYPE;
import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.LINK_TARGET;
import static com.odde.doughnut.entities.QuizQuestionEntity.QuestionType.WHICH_SPEC_HAS_INSTANCE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import com.odde.doughnut.entities.QuizQuestionEntity.QuestionType;
import com.odde.doughnut.entities.validators.ValidateLinkType;
import com.odde.doughnut.models.NoteViewer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;

@Entity
@ValidateLinkType
@Table(name = "link")
@JsonPropertyOrder({"clozeSource", "linkTypeLabel"})
public class Link extends Thingy {
  private Link() {}

  public static Link createLink(
      Note sourceNote,
      Note targetNote,
      User user,
      LinkType linkType,
      Timestamp currentUTCTimestamp) {
    if (linkType == null || linkType == Link.LinkType.NO_LINK) {
      return null;
    }
    Link link = new Link();
    link.setSourceNote(sourceNote);
    link.setTargetNote(targetNote);
    link.setLinkType(linkType);

    return Thing.createThing(user, link, currentUTCTimestamp);
  }

  @OneToOne(mappedBy = "link", cascade = CascadeType.ALL)
  @Getter
  @Setter
  @JsonIgnore
  private Thing thing;

  public enum LinkType {
    NO_LINK(0, "no link", "no link", "", new QuestionType[] {}),
    RELATED_TO(1, "related note", "related to", "related to", new QuestionType[] {}),
    SPECIALIZE(
        2,
        "specification",
        "a specialization of",
        "a generalization of",
        new QuestionType[] {
          LINK_TARGET,
          LINK_SOURCE,
          WHICH_SPEC_HAS_INSTANCE,
          FROM_SAME_PART_AS,
          FROM_DIFFERENT_PART_AS,
          DESCRIPTION_LINK_TARGET
        }),
    APPLICATION(
        3,
        "application",
        "an application of",
        "applied to",
        new QuestionType[] {
          LINK_TARGET,
          LINK_SOURCE_WITHIN_SAME_LINK_TYPE,
          WHICH_SPEC_HAS_INSTANCE,
          FROM_SAME_PART_AS,
          FROM_DIFFERENT_PART_AS,
          DESCRIPTION_LINK_TARGET
        }),

    INSTANCE(
        4,
        "instance",
        "an instance of",
        "has instances",
        new QuestionType[] {
          LINK_TARGET,
          LINK_SOURCE,
          WHICH_SPEC_HAS_INSTANCE,
          FROM_SAME_PART_AS,
          FROM_DIFFERENT_PART_AS,
          DESCRIPTION_LINK_TARGET
        }),
    /*INTEGRATED*/ PART(
        6,
        "part",
        "a part of",
        "has parts",
        new QuestionType[] {
          LINK_TARGET,
          LINK_SOURCE,
          WHICH_SPEC_HAS_INSTANCE,
          FROM_SAME_PART_AS,
          FROM_DIFFERENT_PART_AS,
          DESCRIPTION_LINK_TARGET
        }),
    /*NON INTEGRATED*/ TAGGED_BY(
        8,
        "tag target",
        "tagged by",
        "tagging",
        new QuestionType[] {
          LINK_TARGET,
          LINK_SOURCE,
          WHICH_SPEC_HAS_INSTANCE,
          WHICH_SPEC_HAS_INSTANCE,
          DESCRIPTION_LINK_TARGET
        }),
    ATTRIBUTE(
        10,
        "attribute",
        "an attribute of",
        "has attributes",
        new QuestionType[] {
          LINK_TARGET,
          LINK_SOURCE,
          WHICH_SPEC_HAS_INSTANCE,
          WHICH_SPEC_HAS_INSTANCE,
          DESCRIPTION_LINK_TARGET
        }),

    OPPOSITE_OF(
        12,
        "opposition",
        "the opposite of",
        "the opposite of",
        new QuestionType[] {LINK_TARGET, LINK_SOURCE, DESCRIPTION_LINK_TARGET}),
    AUTHOR_OF(
        14,
        "author",
        "author of",
        "brought by",
        new QuestionType[] {LINK_TARGET, LINK_SOURCE, DESCRIPTION_LINK_TARGET}),
    USES(
        15,
        "user",
        "using",
        "used by",
        new QuestionType[] {
          LINK_TARGET,
          LINK_SOURCE,
          WHICH_SPEC_HAS_INSTANCE,
          FROM_SAME_PART_AS,
          FROM_DIFFERENT_PART_AS,
          DESCRIPTION_LINK_TARGET
        }),
    EXAMPLE_OF(
        17,
        "example",
        "an example of",
        "has examples",
        new QuestionType[] {
          LINK_SOURCE_WITHIN_SAME_LINK_TYPE,
          CLOZE_LINK_TARGET,
          FROM_SAME_PART_AS,
          FROM_DIFFERENT_PART_AS
        }),
    PRECEDES(
        19,
        "precedence",
        "before",
        "after",
        new QuestionType[] {LINK_TARGET, LINK_SOURCE, DESCRIPTION_LINK_TARGET}),
    SIMILAR_TO(
        22,
        "thing",
        "similar to",
        "similar to",
        new QuestionType[] {LINK_TARGET, LINK_SOURCE, DESCRIPTION_LINK_TARGET}),
    CONFUSE_WITH(23, "thing", "confused with", "confused with", new QuestionType[] {});

    @JsonValue public final String label;
    public final String nameOfSource;
    public final Integer id;
    public String reversedLabel;
    @Getter private final QuestionType[] questionTypes;

    LinkType(
        Integer id,
        String nameOfSource,
        String label,
        String reversedLabel,
        QuestionType[] questionTypes) {
      this.nameOfSource = nameOfSource;
      this.label = label;
      this.id = id;
      this.reversedLabel = reversedLabel;
      this.questionTypes = questionTypes;
    }

    private static final Map<Integer, LinkType> idMap =
        Collections.unmodifiableMap(
            Arrays.stream(values()).collect(Collectors.toMap(x -> x.id, x -> x)));

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

  @ManyToOne
  @JoinColumn(name = "source_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Note sourceNote;

  @ManyToOne
  @JoinColumn(name = "target_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Note targetNote;

  @NotNull
  @Column(name = "type_id")
  @Getter
  @Setter
  @JsonIgnore
  private Integer typeId;

  public LinkType getLinkType() {
    return LinkType.fromId(typeId);
  }

  @JsonIgnore
  public String getLinkTypeLabel() {
    return getLinkType().label;
  }

  public void setLinkType(LinkType linkType) {
    if (linkType == null) {
      typeId = null;
      return;
    }
    typeId = linkType.id;
  }

  @JsonIgnore
  public List<Note> getLinkedSiblingsOfSameLinkType(User viewer) {
    return getSiblingLinksOfSameLinkType(viewer).map(Link::getSourceNote).toList();
  }

  @JsonIgnore
  public Stream<Link> getSiblingLinksOfSameLinkType(User viewer) {
    return new NoteViewer(viewer, targetNote)
        .linksOfTypeThroughReverse(getLinkType())
        .filter(l -> !l.equals(this));
  }

  @JsonIgnore
  public List<Note> getPiblingOfTheSameLinkType(User viewer) {
    return getPiblingLinksOfSameLinkType(viewer).stream().map(Link::getTargetNote).toList();
  }

  @JsonIgnore
  public List<Link> getPiblingLinksOfSameLinkType(User viewer) {
    return new NoteViewer(viewer, sourceNote)
        .linksOfTypeThroughDirect(List.of(getLinkType())).stream()
            .filter(l -> !l.equals(this))
            .collect(Collectors.toList());
  }

  @JsonIgnore
  public boolean sourceVisibleAsTargetOrTo(User viewer) {
    if (sourceNote.getNotebook() == targetNote.getNotebook()) return true;
    if (viewer == null) return false;

    return viewer.canReferTo(sourceNote.getNotebook());
  }

  @JsonIgnore
  public List<Link> categoryLinksOfTarget(User viewer) {
    return new NoteViewer(viewer, getTargetNote())
        .linksOfTypeThroughDirect(
            List.of(LinkType.PART, LinkType.INSTANCE, LinkType.SPECIALIZE, LinkType.APPLICATION));
  }
}
