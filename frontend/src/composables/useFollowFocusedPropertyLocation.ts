import type { Ref } from "vue"
import { useRoute, useRouter, type RouteLocationNamedRaw } from "vue-router"
import { noteRouteFamilyNoteId } from "@/routes/noteRouteFamily"
import {
  locationKeepingQuery,
  notePropertyKeyFromRoute,
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"

export function useFollowFocusedPropertyLocation(
  propertyRows: Ref<readonly { key: string }[]>
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
    if (!propertyRows.value.some((row) => row.key === toKey)) {
      return
    }
    return replaceWhenFocusedKeyRemoved(fromKey, (noteId) =>
      notePropertyLocation(noteId, toKey)
    )
  }

  function followFocusedPropertyDelete(removedKey: string) {
    return replaceWhenFocusedKeyRemoved(removedKey, noteShowLocation)
  }

  return { followFocusedPropertyRename, followFocusedPropertyDelete }
}
