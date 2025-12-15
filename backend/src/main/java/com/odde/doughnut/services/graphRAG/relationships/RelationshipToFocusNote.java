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
  TargetSibling,
  InboundReference,
  SubjectOfInboundReference,
  AncestorInContextualPath,
  AncestorInTargetContextualPath,
  TargetOfRelatedChild,
  SiblingOfParent,
  SiblingOfParentOfTarget,
  ChildOfSiblingOfParent,
  ChildOfSiblingOfParentOfTarget,
  InboundReferenceContextualPath,
  SiblingOfSubjectOfInboundReference,
  InboundReferenceToTargetOfRelatedChild,
  SubjectOfTargetSibling,
  RemotelyRelated
}
