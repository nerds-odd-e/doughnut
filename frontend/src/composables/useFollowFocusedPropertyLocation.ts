import type { Ref } from "vue"
import { useRoute, useRouter } from "vue-router"
import { noteRouteFamilyNoteId } from "@/routes/noteRouteFamily"
import {
  locationKeepingQuery,
  notePropertyKeyFromRoute,
  notePropertyLocation,
} from "@/routes/noteShowLocation"

export function useFollowFocusedPropertyLocation(
  propertyRows: Ref<readonly { key: string }[]>
) {
  const route = useRoute()
  const router = useRouter()

  function followFocusedPropertyRename(toKey: string) {
    const fromKey = notePropertyKeyFromRoute(route)
    if (!fromKey || fromKey === toKey || toKey === "") {
      return
    }
    const keys = propertyRows.value.map((row) => row.key)
    if (keys.includes(fromKey) || !keys.includes(toKey)) {
      return
    }
    const noteId = noteRouteFamilyNoteId(route)
    if (noteId === undefined) {
      return
    }
    return router.replace(
      locationKeepingQuery(route, notePropertyLocation(Number(noteId), toKey))
    )
  }

  return { followFocusedPropertyRename }
}
