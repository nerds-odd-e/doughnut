import type { NoteRecallInfo } from "@generated/doughnut-backend-api"
import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import {
  computed,
  inject,
  provide,
  ref,
  watch,
  type ComputedRef,
  type InjectionKey,
  type Ref,
} from "vue"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { hasUnderstandingNoteLevelTracker } from "@/components/recall/assimilationMemoryTrackers"
import type { AssimilateEvent } from "@/composables/useAssimilateUnit"
import {
  useMemoryTrackerActionHandlers,
  type MemoryTrackerActionRequest,
  type MemoryTrackerActionResult,
} from "@/composables/useMemoryTrackerActionHandlers"

export type { MemoryTrackerActionRequest, MemoryTrackerActionResult }

export type MemoryTrackerActions = {
  noteInfoLoaded: Ref<boolean>
  noteRecallInfo: Ref<NoteRecallInfo | null>
  assimilateDisabled: ComputedRef<boolean>
  assimilatingPropertyKey: Ref<string | null>
  showSpellingPopup: ComputedRef<boolean>
  reloadNoteInfo: () => Promise<void>
  assimilate: (event: AssimilateEvent) => Promise<MemoryTrackerActionResult>
  skip: (
    request?: MemoryTrackerActionRequest
  ) => Promise<MemoryTrackerActionResult>
  revive: (
    request: MemoryTrackerActionRequest
  ) => Promise<MemoryTrackerActionResult>
  returnToSequence: (
    request?: MemoryTrackerActionRequest
  ) => Promise<MemoryTrackerActionResult>
  removeFromRecall: (
    request?: MemoryTrackerActionRequest
  ) => Promise<MemoryTrackerActionResult>
  handleSpellingVerified: () => Promise<MemoryTrackerActionResult>
  handleSpellingCancel: () => void
}

export const memoryTrackerActionsKey: InjectionKey<MemoryTrackerActions> =
  Symbol("memoryTrackerActions")

/**
 * Owns the noteRecallInfo fetch/reload for a single note and composes it
 * with the memory-tracker action handlers (assimilate incl.
 * spelling-verification flow, skip, revive, return-to-sequence,
 * remove-from-recall) from `useMemoryTrackerActionHandlers`.
 */
export function useMemoryTrackerActions(
  noteId: Ref<number>
): MemoryTrackerActions {
  const noteInfoLoaded = ref(false)
  const noteRecallInfo = ref<NoteRecallInfo | null>(null)

  const assimilateDisabled = computed(() =>
    hasUnderstandingNoteLevelTracker(noteRecallInfo.value?.memoryTrackers)
  )

  const reloadNoteInfo = async () => {
    const { data, error } = await apiCallWithLoading(() =>
      NoteController.getNoteInfo({ path: { note: noteId.value } })
    )
    if (!error) {
      noteRecallInfo.value = data ?? null
      noteInfoLoaded.value = true
    }
  }

  watch(
    noteId,
    () => {
      reloadNoteInfo()
    },
    { immediate: true }
  )

  const handlers = useMemoryTrackerActionHandlers(
    noteId,
    noteRecallInfo,
    reloadNoteInfo
  )

  return {
    noteInfoLoaded,
    noteRecallInfo,
    assimilateDisabled,
    reloadNoteInfo,
    ...handlers,
  }
}

/** Creates the composable for `note` and provides it to descendants. */
export function provideMemoryTrackerActions(
  noteId: Ref<number>
): MemoryTrackerActions {
  const actions = useMemoryTrackerActions(noteId)
  provide(memoryTrackerActionsKey, actions)
  return actions
}

/**
 * Injects the memory-tracker actions provided by an ancestor (normally
 * `NoteShow.vue`). Falls back to creating a local instance when mounted
 * without a provider (e.g. component tests that mount a panel in isolation).
 */
export function useInjectedMemoryTrackerActions(
  noteId: Ref<number>
): MemoryTrackerActions {
  return inject(
    memoryTrackerActionsKey,
    () => useMemoryTrackerActions(noteId),
    true
  )
}
