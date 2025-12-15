package com.odde.doughnut.services.graphRAG.relationships;

public enum RelationshipToFocusNote {
  Self,

  // Core structure
  Parent,
  Child,
  OlderSibling,
  YoungerSibling,

  // Core relationships
  RelationshipTarget,
  Relationship,

  // Target neighborhood
  SiblingOfTarget,
  ReferenceBy,
  ReferencingNote,
  AncestorInContextualPath,
  AncestorInTargetContextualPath,
  TargetOfRelatedChild,
  SiblingOfParent,
  SiblingOfParentOfTarget,
  ChildOfSiblingOfParent,
  ChildOfSiblingOfParentOfTarget,
  ReferenceByContextualPath,
  SiblingOfReferencingNote,
  ReferenceByToTargetOfRelatedChild,
  SubjectOfTargetSibling,
  RemotelyRelated
}
