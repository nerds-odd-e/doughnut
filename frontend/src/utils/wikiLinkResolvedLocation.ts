import type { RouteLocationNamedRaw } from "vue-router"
import { namedLocationHref } from "@/routes/namedLocationHref"
import {
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import {
  decodeWikiLinkPropertyKey,
  parsePortablePath,
} from "@/utils/portablePath"

/** Resolved wiki click location from the authored target. */
export function locationForResolvedWikiTarget(
  noteId: number,
  authoredTarget: string
): RouteLocationNamedRaw {
  const propertyKey = decodeWikiLinkPropertyKey(
    parsePortablePath(authoredTarget).encodedPropertyKey
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
