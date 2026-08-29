import {
  createMemoryHistory,
  createRouter,
  type RouteLocationNamedRaw,
  type RouteRecordRaw,
} from "vue-router"
import { routeMetadata } from "./routeMetadata"

export function noteShowLocation(noteId: number): RouteLocationNamedRaw {
  return {
    name: "noteShow",
    params: {
      noteId: String(noteId),
    },
  }
}

/** Metadata table with dummy components so href can compile without page imports. */
export const dummyRouteRecordsFromMetadata: RouteRecordRaw[] =
  routeMetadata.map((metadata) => {
    if (metadata.redirect !== undefined) {
      return {
        path: metadata.path,
        redirect: metadata.redirect,
      } as RouteRecordRaw
    }
    return {
      ...metadata,
      component: {
        template: `<div>${metadata.name} (Mock)</div>`,
      },
    }
  }) as RouteRecordRaw[]

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
