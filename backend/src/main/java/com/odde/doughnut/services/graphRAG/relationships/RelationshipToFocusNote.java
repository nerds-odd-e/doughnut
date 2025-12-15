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

  // References
  ReferenceBy,
  ReferencingNote,

  // Contextual inclusion
  ContextAncestor,
  TargetContextAncestor,
  ReferenceContextAncestor,

  // Relationship-derived
  TargetOfRelationship,
  RelationshipOfTargetSibling,

  // Reference + relationship
  ReferencedTargetOfRelationship,

  // Parent neighborhood
  ParentSibling,
  TargetParentSibling,
  ParentSiblingChild,
  TargetParentSiblingChild,

  SiblingOfReferencingNote,
  RemotelyRelated
}
