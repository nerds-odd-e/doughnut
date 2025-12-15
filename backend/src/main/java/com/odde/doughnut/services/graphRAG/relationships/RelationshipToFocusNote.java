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
  TargetOfReifiedChild,
  SiblingOfParent,
  SiblingOfParentOfTarget,
  ChildOfSiblingOfParent,
  ChildOfSiblingOfParentOfTarget,
  InboundReferenceContextualPath,
  SiblingOfSubjectOfInboundReference,
  InboundReferenceToTargetOfReifiedChild,
  TargetSibling,
  SubjectOfTargetSibling,
  RemotelyRelated
}
