import type { NoteRealm } from "@generated/donut-backend-api"
import type NoteStorage from "@/store/NoteStorage"
import { noteRouteFamilyNoteId } from "@/routes/noteRouteFamily"
import { ref, watch, type Ref } from "vue"
import type { RouteLocationNormalizedLoaded } from "vue-router"

/**
 * On note-family routes, keeps the last loaded NoteRealm until the target note
 * is cached, so sidebar chrome does not blank while showNote is in flight.
 */
export function useStickyActiveNoteRealmForRoute(
  route: RouteLocationNormalizedLoaded,
  storageAccessor: Ref<NoteStorage>
) {
  const activeNoteRealm = ref<NoteRealm | undefined>(undefined)

  watch(
    () => {
      const id = Number(noteRouteFamilyNoteId(route))
      if (!Number.isFinite(id)) {
        return { onNoteFamily: false as const, realm: undefined }
      }
      return {
        onNoteFamily: true as const,
        realm: storageAccessor.value.refOfNoteRealm(id).value,
      }
    },
    ({ onNoteFamily, realm }) => {
      if (!onNoteFamily) {
        activeNoteRealm.value = undefined
      } else if (realm !== undefined) {
        activeNoteRealm.value = realm
      }
    },
    { immediate: true }
  )

  return activeNoteRealm
}
