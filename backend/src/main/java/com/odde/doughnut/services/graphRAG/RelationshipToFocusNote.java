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
  NoteInContextualPath,
  NoteInObjectContextualPath,
  ReifiedChildObject,
  ParentSibling,
  ObjectParentSibling,
  ParentSiblingChild,
  ObjectParentSiblingChild,
  InboundReferenceContextualPath,
  InboundReferenceCousin
}
