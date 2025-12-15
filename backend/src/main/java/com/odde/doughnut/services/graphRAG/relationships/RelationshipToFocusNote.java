package com.odde.doughnut.services.graphRAG.relationships;

public enum RelationshipToFocusNote {
  Self,
  Parent,
  Target,
  Child,
  PriorSibling,
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
