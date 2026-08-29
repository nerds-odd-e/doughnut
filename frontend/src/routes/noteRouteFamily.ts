import type { RouteLocationNormalizedLoaded } from "vue-router"

export function isNoteRouteFamily(
  route: Pick<RouteLocationNormalizedLoaded, "meta">
): boolean {
  return route.meta.noteRouteFamily === true
}

export function noteRouteFamilyNoteId(
  route: Pick<RouteLocationNormalizedLoaded, "meta" | "params">
): string | undefined {
  if (!isNoteRouteFamily(route)) {
    return undefined
  }
  const raw = route.params.noteId
  if (raw === undefined) {
    return undefined
  }
  return Array.isArray(raw) ? raw[0] : raw
}
