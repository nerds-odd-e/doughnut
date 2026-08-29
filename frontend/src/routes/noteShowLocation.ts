import {
  createMemoryHistory,
  createRouter,
  type RouteLocationNamedRaw,
  type RouteLocationNormalizedLoaded,
  type RouteLocationRaw,
  type RouteRecordRaw,
  type Router,
} from "vue-router"
import { dummyRouteRecordsFromMetadata } from "./dummyRouteRecords"
import { namedLocationHref } from "./namedLocationHref"
import { isNoteRouteFamily } from "./noteRouteFamily"

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

export function notePropertyLocation(
  noteId: number,
  propertyKey: string
): RouteLocationNamedRaw {
  return {
    name: "noteProperty",
    params: {
      noteId: String(noteId),
      propertyKey,
    },
  }
}

export function notePropertyHref(noteId: number, propertyKey: string): string {
  return namedLocationHref(notePropertyLocation(noteId, propertyKey))
}

export function notePropertyKeyFromRoute(
  route: Pick<RouteLocationNormalizedLoaded, "name" | "params">
): string | undefined {
  if (route.name !== "noteProperty") {
    return undefined
  }
  const raw = route.params.propertyKey
  if (raw === undefined) {
    return undefined
  }
  return Array.isArray(raw) ? raw[0] : raw
}

const unmatchedLocation: RouteRecordRaw = {
  path: "/:pathMatch(.*)*",
  component: { template: "<div />" },
}

const internalNoteRouteClassifierRouter = createRouter({
  history: createMemoryHistory(),
  routes: [...dummyRouteRecordsFromMetadata, unmatchedLocation],
})

const maxRedirects = 8

function resolveFollowingRedirects(router: Router, location: RouteLocationRaw) {
  let current: RouteLocationRaw = location
  for (let i = 0; i < maxRedirects; i++) {
    const resolved = router.resolve(current)
    const redirect = resolved.matched.at(-1)?.redirect
    if (redirect === undefined) {
      return resolved
    }
    current =
      typeof redirect === "function"
        ? redirect(resolved, router.currentRoute.value)
        : redirect
  }
  throw new Error(`Too many redirects resolving ${String(location)}`)
}

export function pathnameLooksLikeInternalNoteFamily(
  pathname: string,
  router: Router = internalNoteRouteClassifierRouter
): boolean {
  return isNoteRouteFamily(resolveFollowingRedirects(router, pathname))
}

/** Bundle-relative note path (`/Folder/Title.md`), not a Donut note-family URL. */
export function hrefLooksLikeConceptNotePath(href: string): boolean {
  if (!href.startsWith("/") || href.startsWith("//")) return false
  const pathname = href.split(/[?#]/, 1)[0] ?? href
  return !pathnameLooksLikeInternalNoteFamily(pathname)
}
