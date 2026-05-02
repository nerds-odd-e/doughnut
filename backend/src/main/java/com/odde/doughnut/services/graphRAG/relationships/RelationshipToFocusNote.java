package com.odde.doughnut.services.graphRAG.relationships;

public enum RelationshipToFocusNote {
  Self,

  // Core structure (tree expansion)
  Parent,
  OlderSibling,
  YoungerSibling,

  ReferencingNote,

  // Contextual inclusion
  ContextAncestor,
  TargetContextAncestor,
  ReferenceContextAncestor,

  RelationshipOfTargetSibling,

  // Parent neighborhood
  ParentSibling,
  TargetParentSibling,
  ParentSiblingChild,
  TargetParentSiblingChild,

  SiblingOfReferencingNote,
}
