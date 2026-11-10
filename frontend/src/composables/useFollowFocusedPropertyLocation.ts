import type { Ref } from "vue"
import type { PropertyRow } from "@/utils/noteContentFrontmatter"
import { propertyItemIndex } from "@/utils/noteProperties"
import { useRoute, useRouter, type RouteLocationNamedRaw } from "vue-router"
import { noteRouteFamilyNoteId } from "@/routes/noteRouteFamily"
import {
  locationKeepingQuery,
  notePropertyKeyFromRoute,
  notePropertyItemFromRoute,
  notePropertyItemLocation,
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"

export function useFollowFocusedPropertyLocation(
  propertyRows: Ref<readonly PropertyRow[]>
) {
  const route = useRoute()
  const router = useRouter()

  function replaceWhenFocusedKeyRemoved(
    expectedKey: string,
    nextLocation: (noteId: number) => RouteLocationNamedRaw
  ) {
    const fromKey = notePropertyKeyFromRoute(route)
    if (fromKey !== expectedKey) {
      return
    }
    if (propertyRows.value.some((row) => row.key === fromKey)) {
      return
    }
    const noteId = noteRouteFamilyNoteId(route)
    if (noteId === undefined) {
      return
    }
    return router.replace(
      locationKeepingQuery(route, nextLocation(Number(noteId)))
    )
  }

  function followFocusedPropertyRename(toKey: string) {
    const fromKey = notePropertyKeyFromRoute(route)
    if (!fromKey || fromKey === toKey || toKey === "") {
      return
    }
    const row = propertyRows.value.find((row) => row.key === toKey)
    if (!row) {
      return
    }
    const item = notePropertyItemFromRoute(route)
    return replaceWhenFocusedKeyRemoved(fromKey, (noteId) =>
      item &&
      row.value.kind === "list" &&
      propertyItemIndex(row.value.items, item) >= 0
        ? notePropertyItemLocation(noteId, toKey, item)
        : notePropertyLocation(noteId, toKey)
    )
  }

  function followFocusedPropertyDelete(removedKey: string) {
    return replaceWhenFocusedKeyRemoved(removedKey, noteShowLocation)
  }

  return { followFocusedPropertyRename, followFocusedPropertyDelete }
}
