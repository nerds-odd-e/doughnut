package com.odde.doughnut.services.graphRAG;

public enum RelationshipToFocusNote {
  Self,
  Parent,
  Object,
  Child,
  PriorSibling,
  YoungerSibling,
  InboundReference,
  InboundReferenceSubject,
  AncestorInContextualPath,
  AncestorInObjectContextualPath,
  ObjectOfReifiedChild,
  SiblingOfParent,
  SiblingOfParentOfObject,
  ChildOfSiblingOfParent,
  ChildOfSiblingOfParentOfObject,
  InboundReferenceContextualPath,
  InboundReferenceCousin
}
