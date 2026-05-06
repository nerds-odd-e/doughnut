import type { RelationshipLiteralSearchHit } from "@generated/doughnut-backend-api"

/** Stable key for deduping hits and Vue list keys. */
export function relationshipLiteralSearchHitKey(
  hit: RelationshipLiteralSearchHit
): string {
  if (
    hit.hitKind === "NOTE" &&
    hit.noteSearchResult?.noteTopology?.id != null
  ) {
    return `n:${hit.noteSearchResult.noteTopology.id}`
  }
  if (hit.hitKind === "FOLDER" && hit.folderId != null) {
    return `f:${hit.folderId}`
  }
  if (hit.hitKind === "NOTEBOOK" && hit.notebookId != null) {
    return `nb:${hit.notebookId}`
  }
  return `x:${JSON.stringify(hit)}`
}
