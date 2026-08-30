import type { RouteLocationNamedRaw } from "vue-router"
import { namedLocationHref } from "@/routes/namedLocationHref"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { parseWikiLinkAuthoredTarget } from "@/utils/wikiLinkAuthoredTarget"

/** Resolved wiki/path-Markdown click location from the authored target. */
export function locationForResolvedWikiTarget(
  noteId: number,
  authoredTarget: string
): RouteLocationNamedRaw {
  parseWikiLinkAuthoredTarget(authoredTarget)
  return noteShowLocation(noteId)
}

export function hrefForResolvedWikiTarget(
  noteId: number,
  authoredTarget: string
): string {
  return namedLocationHref(
    locationForResolvedWikiTarget(noteId, authoredTarget)
  )
}
