import {
  createMemoryHistory,
  createRouter,
  type RouteLocationNamedRaw,
} from "vue-router"
import { dummyRouteRecordsFromMetadata } from "./dummyRouteRecords"

export function noteShowLocation(noteId: number): RouteLocationNamedRaw {
  return {
    name: "noteShow",
    params: {
      noteId: String(noteId),
    },
  }
}

const noteShowHrefRouter = createRouter({
  history: createMemoryHistory(),
  routes: dummyRouteRecordsFromMetadata,
})

export function noteShowHref(noteId: number): string {
  return noteShowHrefRouter.resolve(noteShowLocation(noteId)).href
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
