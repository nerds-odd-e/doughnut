package com.odde.doughnut.services.graphRAG;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
public class CandidateNote {
  private final Note note;
  private final int depthFetched;
  private final List<RelationshipToFocusNote> discoveryPath;
  @Setter private RelationshipToFocusNote relationshipType;
  @JsonIgnore @Setter private double relevanceScore;

  public CandidateNote(
      Note note, int depthFetched, List<RelationshipToFocusNote> discoveryPath) {
    this.note = note;
    this.depthFetched = depthFetched;
    this.discoveryPath = new ArrayList<>(discoveryPath);
    this.relationshipType = deriveRelationshipType(discoveryPath);
  }

  public static RelationshipToFocusNote deriveRelationshipType(
      List<RelationshipToFocusNote> path) {
    if (path.isEmpty()) {
      return RelationshipToFocusNote.Self;
    }

    if (path.size() == 1) {
      RelationshipToFocusNote first = path.get(0);
      if (first == RelationshipToFocusNote.Parent
          || first == RelationshipToFocusNote.Child
          || first == RelationshipToFocusNote.Object
          || first == RelationshipToFocusNote.InboundReference) {
        return first;
      }
    }

    if (path.size() == 2) {
      RelationshipToFocusNote first = path.get(0);
      RelationshipToFocusNote second = path.get(1);

      if (first == RelationshipToFocusNote.Parent && second == RelationshipToFocusNote.Parent) {
        return RelationshipToFocusNote.AncestorInContextualPath;
      }
      if (first == RelationshipToFocusNote.Object && second == RelationshipToFocusNote.Parent) {
        return RelationshipToFocusNote.AncestorInObjectContextualPath;
      }
      if (first == RelationshipToFocusNote.Child && second == RelationshipToFocusNote.Object) {
        return RelationshipToFocusNote.ObjectOfReifiedChild;
      }
      if (first == RelationshipToFocusNote.InboundReference
          && second == RelationshipToFocusNote.Parent) {
        return RelationshipToFocusNote.SubjectOfInboundReference;
      }
      if (first == RelationshipToFocusNote.Parent && second == RelationshipToFocusNote.Child) {
        return RelationshipToFocusNote.SiblingOfParent;
      }
      if (first == RelationshipToFocusNote.Object
          && second == RelationshipToFocusNote.Parent) {
        return RelationshipToFocusNote.SiblingOfParentOfObject;
      }
      if (first == RelationshipToFocusNote.InboundReference
          && second == RelationshipToFocusNote.Parent) {
        return RelationshipToFocusNote.InboundReferenceContextualPath;
      }
    }

    if (path.size() == 3) {
      RelationshipToFocusNote first = path.get(0);
      RelationshipToFocusNote second = path.get(1);
      RelationshipToFocusNote third = path.get(2);

      if (first == RelationshipToFocusNote.Parent
          && second == RelationshipToFocusNote.Child
          && third == RelationshipToFocusNote.Child) {
        return RelationshipToFocusNote.ChildOfSiblingOfParent;
      }
      if (first == RelationshipToFocusNote.Object
          && second == RelationshipToFocusNote.Parent
          && third == RelationshipToFocusNote.Child) {
        return RelationshipToFocusNote.ChildOfSiblingOfParentOfObject;
      }
      if (first == RelationshipToFocusNote.InboundReference
          && second == RelationshipToFocusNote.Parent
          && third == RelationshipToFocusNote.Child) {
        return RelationshipToFocusNote.SiblingOfSubjectOfInboundReference;
      }
      if (first == RelationshipToFocusNote.Child
          && second == RelationshipToFocusNote.Object
          && third == RelationshipToFocusNote.InboundReference) {
        return RelationshipToFocusNote.InboundReferenceToObjectOfReifiedChild;
      }
      if (first == RelationshipToFocusNote.Child && second == RelationshipToFocusNote.Child) {
        return RelationshipToFocusNote.GrandChild;
      }
    }

    if (path.size() >= 2) {
      if (path.get(0) == RelationshipToFocusNote.Child
          && path.size() >= 2
          && path.get(1) == RelationshipToFocusNote.Child) {
        return RelationshipToFocusNote.GrandChild;
      }
    }

    if (path.size() >= 3) {
      return RelationshipToFocusNote.RemotelyRelated;
    }

    return RelationshipToFocusNote.RemotelyRelated;
  }

  public void updateRelationshipTypeIfShorterPath(List<RelationshipToFocusNote> newPath) {
    if (newPath.size() < discoveryPath.size()) {
      this.relationshipType = deriveRelationshipType(newPath);
    } else if (newPath.size() == discoveryPath.size()) {
      RelationshipToFocusNote newType = deriveRelationshipType(newPath);
      if (hasHigherPriority(newType, this.relationshipType)) {
        this.relationshipType = newType;
      }
    }
  }

  private boolean hasHigherPriority(
      RelationshipToFocusNote type1, RelationshipToFocusNote type2) {
    int priority1 = getPriority(type1);
    int priority2 = getPriority(type2);
    return priority1 < priority2;
  }

  private int getPriority(RelationshipToFocusNote type) {
    switch (type) {
      case Self:
      case Parent:
      case Child:
      case Object:
      case InboundReference:
        return 1;
      case PriorSibling:
      case YoungerSibling:
      case ObjectOfReifiedChild:
      case SubjectOfInboundReference:
        return 2;
      case AncestorInContextualPath:
      case AncestorInObjectContextualPath:
      case SiblingOfParent:
      case SiblingOfParentOfObject:
      case ChildOfSiblingOfParent:
      case ChildOfSiblingOfParentOfObject:
      case InboundReferenceContextualPath:
      case SiblingOfSubjectOfInboundReference:
      case InboundReferenceToObjectOfReifiedChild:
        return 3;
      case GrandChild:
      case RemotelyRelated:
      default:
        return 4;
    }
  }
}

