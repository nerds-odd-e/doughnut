package com.odde.doughnut.entities;

import static com.odde.doughnut.entities.QuizQuestion.QuestionType.CLOZE_LINK_TARGET;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.DESCRIPTION_LINK_TARGET;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.FROM_DIFFERENT_PART_AS;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.FROM_SAME_PART_AS;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.LINK_SOURCE;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.LINK_SOURCE_EXCLUSIVE;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.LINK_TARGET;
import static com.odde.doughnut.entities.QuizQuestion.QuestionType.WHICH_SPEC_HAS_INSTANCE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.odde.doughnut.entities.QuizQuestion.QuestionType;
import com.odde.doughnut.models.NoteViewer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "link")
public class Link {

  public enum LinkType {
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
          LINK_SOURCE,
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
          LINK_SOURCE_EXCLUSIVE,
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
          LINK_SOURCE_EXCLUSIVE,
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
        new QuestionType[] {
          LINK_TARGET, LINK_SOURCE, LINK_SOURCE_EXCLUSIVE, DESCRIPTION_LINK_TARGET
        }),
    USES(
        15,
        "user",
        "using",
        "used by",
        new QuestionType[] {
          LINK_TARGET,
          LINK_SOURCE,
          LINK_SOURCE_EXCLUSIVE,
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
          LINK_SOURCE,
          LINK_SOURCE_EXCLUSIVE,
          CLOZE_LINK_TARGET,
          FROM_SAME_PART_AS,
          FROM_DIFFERENT_PART_AS
        }),
    PRECEDES(
        19,
        "precedence",
        "before",
        "after",
        new QuestionType[] {
          LINK_TARGET, LINK_SOURCE, LINK_SOURCE_EXCLUSIVE, DESCRIPTION_LINK_TARGET
        }),
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
    private DoughPredicate doughPredicate;

    LinkType(
        Integer id,
        String nameOfSource,
        String label,
        String reversedLabel,
        QuestionType[] questionTypes) {
      this.doughPredicate = new DoughPredicate(this);
      this.nameOfSource = nameOfSource;
      this.label = label;
      this.id = id;
      this.reversedLabel = reversedLabel;
      this.questionTypes = questionTypes;
    }

    private static final Map<Integer, LinkType> idMap =
        Collections.unmodifiableMap(
            Arrays.stream(values()).collect(Collectors.toMap(x -> x.id, x -> x)));

    public static Stream<LinkType> openTypes() {
      final Link.LinkType[] openTypes = {
        Link.LinkType.TAGGED_BY, Link.LinkType.INSTANCE, Link.LinkType.PART
      };
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
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "source_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Note sourceNote;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "target_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Note targetNote;

  @NotNull
  @Column(name = "type_id")
  @Getter
  @Setter
  private Integer typeId;

  @JsonIgnore
  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "user_id", referencedColumnName = "id")
  @Getter
  @Setter
  private User user;

  @Column(name = "created_at")
  @Getter
  @Setter
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  @OneToMany(mappedBy = "link", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonIgnore
  private final List<ReviewPoint> reviewPointEntities = new ArrayList<>();

  @JsonIgnore
  public LinkType getLinkType() {
    return LinkType.fromId(typeId);
  }

  public String getLinkTypeLabel() {
    return getLinkType().label;
  }

  public void setLinkType(LinkType linkType) {
    if (linkType == null) {
      typeId = null;
    }
    typeId = linkType.id;
  }

  @JsonIgnore
  public List<Note> getCousinsOfSameLinkType(User viewer) {
    return getCousinLinksOfSameLinkType(viewer).stream().map(Link::getSourceNote).toList();
  }

  @JsonIgnore
  public List<Link> getCousinLinksOfSameLinkType(User viewer) {
    return new NoteViewer(viewer, targetNote)
        .linksOfTypeThroughReverse(getLinkType())
        .filter(l -> !l.equals(this))
        .collect(Collectors.toList());
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
  public List<LinkType> getPossibleLinkTypes() {
    final List<LinkType> existingTypes =
        sourceNote.getLinks().stream()
            .filter(l -> l.targetNote == targetNote)
            .map(Link::getLinkType)
            .collect(Collectors.toUnmodifiableList());
    return Arrays.stream(LinkType.values())
        .filter(lt -> lt == getLinkType() || !existingTypes.contains(lt))
        .collect(Collectors.toList());
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
  public List<Link> categoryLinksOfTarget(User viewer) {
    return new NoteViewer(viewer, getTargetNote())
        .linksOfTypeThroughDirect(
            List.of(LinkType.PART, LinkType.INSTANCE, LinkType.SPECIALIZE, LinkType.APPLICATION));
  }

  @JsonIgnore
  public List<Link> getReverseLinksOfCousins(User user, LinkType linkType) {
    return getCousinLinksOfSameLinkType(user).stream()
        .flatMap(p -> new NoteViewer(user, p.getSourceNote()).linksOfTypeThroughReverse(linkType))
        .collect(Collectors.toList());
  }
}
