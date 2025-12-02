package com.odde.doughnut.services.graphRAG.relationships;

public enum RelationshipToFocusNote {
  Self,
  Parent,
  Object,
  Child,
  PriorSibling,
  YoungerSibling,
  InboundReference,
  SubjectOfInboundReference,
  AncestorInContextualPath,
  AncestorInObjectContextualPath,
  ObjectOfReifiedChild,
  SiblingOfParent,
  SiblingOfParentOfObject,
  ChildOfSiblingOfParent,
  ChildOfSiblingOfParentOfObject,
  InboundReferenceContextualPath,
  SiblingOfSubjectOfInboundReference,
  InboundReferenceToObjectOfReifiedChild,
  GrandChild,
  RemotelyRelated
}
