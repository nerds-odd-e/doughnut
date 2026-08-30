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
import { isNoteRouteFamily, noteRouteFamilyNoteId } from "./noteRouteFamily"

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

export function locationKeepingQuery(
  route: Pick<RouteLocationNormalizedLoaded, "query">,
  location: RouteLocationNamedRaw
): RouteLocationNamedRaw {
  return {
    ...location,
    query: { ...route.query },
  }
}

export function currentRouteSettingConversation(
  route: Pick<RouteLocationNormalizedLoaded, "name" | "params" | "query">,
  conversationOpen: boolean
): RouteLocationNamedRaw {
  const query = { ...route.query }
  if (conversationOpen) {
    query.conversation = "true"
  } else {
    delete query.conversation
  }
  return {
    name: route.name ?? undefined,
    params: { ...route.params },
    query,
  }
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

export type InternalNoteFamilyHref = {
  noteId: number
  propertyKey: string | undefined
}

function pathnameFromHref(href: string): string | undefined {
  try {
    return new URL(href, "https://example.invalid").pathname
  } catch {
    return undefined
  }
}

/** Parse a pasted SPA href through the route table (property key decoded once). */
export function resolveInternalNoteFamilyFromHref(
  href: string,
  router: Router = internalNoteRouteClassifierRouter
): InternalNoteFamilyHref | undefined {
  const pathname = pathnameFromHref(href)
  if (pathname === undefined) {
    return undefined
  }
  const resolved = resolveFollowingRedirects(router, pathname)
  if (!isNoteRouteFamily(resolved)) {
    return undefined
  }
  const noteIdRaw = noteRouteFamilyNoteId(resolved)
  if (noteIdRaw === undefined) {
    return undefined
  }
  const noteId = Number(noteIdRaw)
  if (!Number.isFinite(noteId)) {
    return undefined
  }
  return {
    noteId,
    propertyKey: notePropertyKeyFromRoute({
      name: resolved.name ?? undefined,
      params: resolved.params,
    }),
  }
}

export function hrefLooksLikeNoteShow(
  href: string | null | undefined
): boolean {
  if (!href?.trim()) return false
  const family = resolveInternalNoteFamilyFromHref(href)
  return family !== undefined && family.propertyKey === undefined
}

export function resolveNotePropertyFromHref(
  href: string
): { noteId: number; propertyKey: string } | undefined {
  const family = resolveInternalNoteFamilyFromHref(href)
  if (!family?.propertyKey) {
    return undefined
  }
  return { noteId: family.noteId, propertyKey: family.propertyKey }
}

export function pathnameLooksLikeInternalNoteFamily(
  pathname: string,
  router: Router = internalNoteRouteClassifierRouter
): boolean {
  return resolveInternalNoteFamilyFromHref(pathname, router) !== undefined
}

/** Bundle-relative note path (`/Folder/Title.md`), not a Donut note-family URL. */
export function hrefLooksLikeConceptNotePath(href: string): boolean {
  if (!href.startsWith("/") || href.startsWith("//")) return false
  const pathname = href.split(/[?#]/, 1)[0] ?? href
  return !pathnameLooksLikeInternalNoteFamily(pathname)
}
