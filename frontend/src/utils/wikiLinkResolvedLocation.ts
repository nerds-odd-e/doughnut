import type { RouteLocationNamedRaw } from "vue-router"
import { namedLocationHref } from "@/routes/namedLocationHref"
import {
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import {
  decodeWikiLinkPropertyKey,
  parseWikiLinkAuthoredTarget,
} from "@/utils/wikiLinkAuthoredTarget"

/** Resolved wiki/path-Markdown click location from the authored target. */
export function locationForResolvedWikiTarget(
  noteId: number,
  authoredTarget: string
): RouteLocationNamedRaw {
  const propertyKey = decodeWikiLinkPropertyKey(
    parseWikiLinkAuthoredTarget(authoredTarget).encodedPropertyKey
  )
  if (propertyKey !== undefined) {
    return notePropertyLocation(noteId, propertyKey)
  }
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
