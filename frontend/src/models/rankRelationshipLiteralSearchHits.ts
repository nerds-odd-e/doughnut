import type { RelationshipLiteralSearchHit } from "@generated/donut-backend-api"
import { relationshipLiteralSearchHitKey } from "./relationshipLiteralSearchHitKey"

function hitDistance(h: RelationshipLiteralSearchHit): number {
  if (h.hitKind === "NOTE" && h.noteSearchResult) {
    return h.noteSearchResult.distance ?? Number.POSITIVE_INFINITY
  }
  if (h.hitKind === "FOLDER" || h.hitKind === "NOTEBOOK") {
    return h.distance ?? Number.POSITIVE_INFINITY
  }
  return Number.POSITIVE_INFINITY
}

function titleForSort(h: RelationshipLiteralSearchHit): string {
  if (h.hitKind === "NOTE" && h.noteSearchResult?.noteTopology?.title) {
    return h.noteSearchResult.noteTopology.title.trim().toLowerCase()
  }
  if (h.hitKind === "FOLDER" && h.folderName) {
    return h.folderName.trim().toLowerCase()
  }
  if (h.hitKind === "NOTEBOOK" && h.notebookName) {
    return h.notebookName.trim().toLowerCase()
  }
  return ""
}

function notebookIdOf(h: RelationshipLiteralSearchHit): number | undefined {
  if (h.hitKind === "NOTE" && h.noteSearchResult) {
    return h.noteSearchResult.notebookId
  }
  if (h.hitKind === "FOLDER" || h.hitKind === "NOTEBOOK") {
    return h.notebookId
  }
  return
}

function hitKindRank(kind: RelationshipLiteralSearchHit["hitKind"]): number {
  if (kind === "NOTE") return 0
  if (kind === "NOTEBOOK") return 1
  if (kind === "FOLDER") return 2
  return 3
}

export function rankRelationshipLiteralSearchHits(
  hits: RelationshipLiteralSearchHit[],
  currentNotebookId?: number,
  searchKeyLower = ""
): RelationshipLiteralSearchHit[] {
  const byKey = new Map<string, RelationshipLiteralSearchHit>()
  hits.forEach((h) => {
    const key = relationshipLiteralSearchHitKey(h)
    const prev = byKey.get(key)
    if (!prev || hitDistance(h) < hitDistance(prev)) byKey.set(key, h)
  })

  return Array.from(byKey.values()).sort((a, b) => {
    const ta = titleForSort(a)
    const tb = titleForSort(b)
    const exactA = searchKeyLower !== "" && ta === searchKeyLower
    const exactB = searchKeyLower !== "" && tb === searchKeyLower
    if (exactA !== exactB) return exactA ? -1 : 1

    const da = hitDistance(a)
    const db = hitDistance(b)
    const distDiff = da - db
    if (!Number.isNaN(distDiff) && Math.abs(distDiff) > 1e-6) {
      return distDiff
    }

    if (currentNotebookId !== undefined) {
      const aNb = notebookIdOf(a)
      const bNb = notebookIdOf(b)
      const aSame = aNb === currentNotebookId
      const bSame = bNb === currentNotebookId
      const nb = Number(bSame) - Number(aSame)
      if (nb !== 0) return nb
    }

    const lenDiff = ta.length - tb.length
    if (lenDiff !== 0) return lenDiff
    const cmp = ta.localeCompare(tb, undefined, {
      sensitivity: "base",
    })
    if (cmp !== 0) return cmp

    if (a.hitKind === "NOTE" && b.hitKind === "NOTE") {
      return (
        (a.noteSearchResult!.noteTopology!.id as number) -
        (b.noteSearchResult!.noteTopology!.id as number)
      )
    }
    if (a.hitKind === "FOLDER" && b.hitKind === "FOLDER") {
      return (a.folderId ?? 0) - (b.folderId ?? 0)
    }
    if (a.hitKind === "NOTEBOOK" && b.hitKind === "NOTEBOOK") {
      return (a.notebookId ?? 0) - (b.notebookId ?? 0)
    }
    return hitKindRank(a.hitKind) - hitKindRank(b.hitKind)
  })
}
