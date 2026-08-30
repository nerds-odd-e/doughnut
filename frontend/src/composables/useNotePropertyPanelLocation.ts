import { toValue, type MaybeRefOrGetter } from "vue"
import { useRoute, useRouter } from "vue-router"
import { noteRouteFamilyNoteId } from "@/routes/noteRouteFamily"
import {
  locationKeepingQuery,
  notePropertyKeyFromRoute,
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"

export function useNotePropertyPanelLocation(
  propertyKey: MaybeRefOrGetter<string>
) {
  const route = useRoute()
  const router = useRouter()

  function currentNoteId() {
    return Number(noteRouteFamilyNoteId(route))
  }

  function isCurrentPropertyPanel() {
    return notePropertyKeyFromRoute(route) === toValue(propertyKey)
  }

  function replaceToPropertyPanel() {
    if (isCurrentPropertyPanel()) {
      return
    }
    return router.replace(
      locationKeepingQuery(
        route,
        notePropertyLocation(currentNoteId(), toValue(propertyKey))
      )
    )
  }

  function replaceToNoteShow() {
    return router.replace(
      locationKeepingQuery(route, noteShowLocation(currentNoteId()))
    )
  }

  function togglePropertyPanel() {
    if (isCurrentPropertyPanel()) {
      return replaceToNoteShow()
    }
    return replaceToPropertyPanel()
  }

  return { togglePropertyPanel }
}
