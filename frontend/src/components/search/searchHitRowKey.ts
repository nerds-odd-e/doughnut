import type { RelationshipLiteralSearchHit } from "@generated/doughnut-backend-api"

export function searchHitRowKey(hit: RelationshipLiteralSearchHit): string {
  if (
    hit.hitKind === "NOTE" &&
    hit.noteSearchResult?.noteTopology?.id != null
  ) {
    return `n-${hit.noteSearchResult.noteTopology.id}`
  }
  if (hit.hitKind === "FOLDER" && hit.folderId != null) {
    return `f-${hit.folderId}`
  }
  return "x"
}
