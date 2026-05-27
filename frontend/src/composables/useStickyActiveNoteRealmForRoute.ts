import type { NoteRealm } from "@generated/doughnut-backend-api"
import type NoteStorage from "@/store/NoteStorage"
import { ref, watch, type Ref } from "vue"
import type { RouteLocationNormalizedLoaded } from "vue-router"

/**
 * On noteShow, keeps the last loaded NoteRealm until the target note is cached,
 * so sidebar chrome does not blank while showNote is in flight.
 */
export function useStickyActiveNoteRealmForRoute(
  route: RouteLocationNormalizedLoaded,
  storageAccessor: Ref<NoteStorage>
) {
  const activeNoteRealm = ref<NoteRealm | undefined>(undefined)

  watch(
    () => {
      if (route.name !== "noteShow") {
        return { onNoteShow: false as const, realm: undefined }
      }
      const id = Number(route.params.noteId)
      if (!Number.isFinite(id)) {
        return { onNoteShow: false as const, realm: undefined }
      }
      return {
        onNoteShow: true as const,
        realm: storageAccessor.value.refOfNoteRealm(id).value,
      }
    },
    ({ onNoteShow, realm }) => {
      if (!onNoteShow) {
        activeNoteRealm.value = undefined
      } else if (realm !== undefined) {
        activeNoteRealm.value = realm
      }
    },
    { immediate: true }
  )

  return activeNoteRealm
}
