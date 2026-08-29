import type { RouteLocationNamedRaw } from "vue-router"
import { namedLocationHref } from "./namedLocationHref"

export function noteShowLocation(noteId: number): RouteLocationNamedRaw {
  return {
    name: "noteShow",
    params: {
      noteId: String(noteId),
    },
  }
}

export function noteShowHref(noteId: number): string {
  return namedLocationHref(noteShowLocation(noteId))
}

export function pathnameLooksLikeInternalNoteShow(pathname: string): boolean {
  return (
    /^\/d\/n\/\d+(\/|$)/.test(pathname) ||
    /^\/n\/\d+(\/|$)/.test(pathname) ||
    /^\/n\d+$/.test(pathname)
  )
}

/** Bundle-relative note path (`/Folder/Title.md`), not a Donut note-show URL. */
export function hrefLooksLikeConceptNotePath(href: string): boolean {
  if (!href.startsWith("/") || href.startsWith("//")) return false
  const pathname = href.split(/[?#]/, 1)[0] ?? href
  return !pathnameLooksLikeInternalNoteShow(pathname)
}
