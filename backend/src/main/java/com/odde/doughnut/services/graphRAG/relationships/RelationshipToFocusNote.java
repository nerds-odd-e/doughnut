package com.odde.doughnut.services.graphRAG.relationships;

public enum RelationshipToFocusNote {
  Self,

  // Core structure
  Parent,
  Target,
  Child,
  Relationship,
  OlderSibling,
  YoungerSibling,
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
  TargetSibling,
  SubjectOfTargetSibling,
  RemotelyRelated
}
