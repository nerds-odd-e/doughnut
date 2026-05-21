import type { NoteRealm } from "@generated/doughnut-backend-api"
import type { Decorator } from "@storybook/vue3-vite"
import { onUnmounted } from "vue"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import createNoteStorage from "@/store/createNoteStorage"
import {
  mockAssimilationStoryApis,
  restoreStorySdkMocks,
} from "../storySdkMocks"

/** Fresh note storage and optional preloaded realms for stories that use NoteRealmLoader. */
export function withNoteStorage(
  seedRealms?: (args: Record<string, unknown>) => NoteRealm[] | undefined
): Decorator {
  return (story, context) => ({
    setup() {
      useStorageAccessor().value = createNoteStorage()
      const realms = seedRealms?.(context.args) ?? []
      if (realms.length > 0) {
        mockAssimilationStoryApis(realms)
        const storage = useStorageAccessor()
        for (const realm of realms) {
          storage.value.refreshNoteRealm(realm)
        }
      }
      onUnmounted(restoreStorySdkMocks)
    },
    components: { story },
    template: "<story />",
  })
}

export const assimilationPageStorageDecorator = withNoteStorage((args) => {
  const notes = args.notes
  if (!Array.isArray(notes) || notes.length === 0) return []
  return notes as NoteRealm[]
})
