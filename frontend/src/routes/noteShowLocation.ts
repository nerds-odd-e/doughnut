import type { RouteLocationNamedRaw } from "vue-router"

export function noteShowLocation(noteId: number): RouteLocationNamedRaw {
  return {
    name: "noteShow",
    params: {
      noteId: String(noteId),
    },
  }
}

export function noteShowHref(noteId: number): string {
  return `/d/n/${noteId}`
}
