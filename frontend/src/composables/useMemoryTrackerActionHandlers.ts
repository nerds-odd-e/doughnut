import type { NoteRecallInfo } from "@generated/donut-backend-api"
import { computed, ref, type ComputedRef, type Ref } from "vue"
import usePopups from "@/components/commons/Popups/usePopups"
import {
  useAssimilateUnit,
  type AssimilateEvent,
} from "@/composables/useAssimilateUnit"
import {
  SEQUENCE_SKIP_CONFIRM,
  useAssimilationSequenceSkip,
} from "@/composables/useAssimilationSequenceSkip"
import { activeUnderstandingTrackers } from "@/components/recall/assimilationMemoryTrackers"
import {
  trackersToRevive,
  useReviveMemoryTracker,
} from "@/composables/useReviveMemoryTracker"
import {
  REMOVE_FROM_RECALL_CONFIRM,
  useRemoveFromRecall,
} from "@/composables/useRemoveFromRecall"

export type MemoryTrackerActionRequest = { propertyKey?: string }

export type MemoryTrackerActionResult = {
  completed: boolean
  navigated: boolean
}

const notCompleted: MemoryTrackerActionResult = {
  completed: false,
  navigated: false,
}

export type MemoryTrackerActionHandlers = {
  assimilatingPropertyKey: Ref<string | null>
  showSpellingPopup: ComputedRef<boolean>
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

/**
 * Builds the five memory-tracker action handlers (assimilate incl. the
 * spelling-verification flow, skip, revive, return-to-sequence,
 * remove-from-recall) for a single note. `reloadNoteInfo` refreshes the
 * shared `noteRecallInfo` owned by the caller after each successful action.
 */
export function useMemoryTrackerActionHandlers(
  noteId: Ref<number>,
  noteRecallInfo: Ref<NoteRecallInfo | null>,
  reloadNoteInfo: () => Promise<void>
): MemoryTrackerActionHandlers {
  const { popups } = usePopups()
  const { assimilateUnit } = useAssimilateUnit()
  const { skipFromAssimilationSequence, returnToAssimilationSequence } =
    useAssimilationSequenceSkip()
  const { reviveMemoryTrackers } = useReviveMemoryTracker()
  const { removeMemoryTrackersFromRecall } = useRemoveFromRecall()

  const assimilatingPropertyKey = ref<string | null>(null)
  const pendingAssimilateAfterSpelling = ref<AssimilateEvent | null>(null)
  const showSpellingPopup = computed(
    () => pendingAssimilateAfterSpelling.value !== null
  )

  const doAssimilate = async (
    event: AssimilateEvent
  ): Promise<MemoryTrackerActionResult> => {
    assimilatingPropertyKey.value = event.propertyKey ?? null
    try {
      const result = await assimilateUnit({ noteId: noteId.value, ...event })
      if (!result.success) {
        return notCompleted
      }
      await reloadNoteInfo()
      return { completed: true, navigated: result.navigated }
    } finally {
      assimilatingPropertyKey.value = null
    }
  }

  const assimilate = async (
    event: AssimilateEvent
  ): Promise<MemoryTrackerActionResult> => {
    if (event.assimilateAsSpelling) {
      pendingAssimilateAfterSpelling.value = event
      return notCompleted
    }
    return doAssimilate(event)
  }

  const handleSpellingVerified =
    async (): Promise<MemoryTrackerActionResult> => {
      const pending = pendingAssimilateAfterSpelling.value
      pendingAssimilateAfterSpelling.value = null
      if (!pending) {
        return notCompleted
      }
      return doAssimilate(pending)
    }

  const handleSpellingCancel = () => {
    pendingAssimilateAfterSpelling.value = null
  }

  const skip = async ({
    propertyKey,
  }: MemoryTrackerActionRequest = {}): Promise<MemoryTrackerActionResult> => {
    const confirmed = await popups.confirm(SEQUENCE_SKIP_CONFIRM)
    if (!confirmed) {
      return notCompleted
    }

    const result = await skipFromAssimilationSequence(noteId.value, propertyKey)
    if (!result.success) {
      return notCompleted
    }

    await reloadNoteInfo()
    return { completed: true, navigated: result.navigated }
  }

  const revive = async ({
    propertyKey,
  }: MemoryTrackerActionRequest): Promise<MemoryTrackerActionResult> => {
    const trackers = trackersToRevive(noteRecallInfo.value, propertyKey)
    if (trackers.length === 0) {
      return notCompleted
    }

    const success = await reviveMemoryTrackers(trackers)
    if (!success) {
      return notCompleted
    }

    await reloadNoteInfo()
    return { completed: true, navigated: false }
  }

  const returnToSequence = async ({
    propertyKey,
  }: MemoryTrackerActionRequest = {}): Promise<MemoryTrackerActionResult> => {
    const success = await returnToAssimilationSequence(
      noteId.value,
      propertyKey
    )
    if (!success) {
      return notCompleted
    }

    await reloadNoteInfo()
    return { completed: true, navigated: false }
  }

  const removeFromRecall = async ({
    propertyKey,
  }: MemoryTrackerActionRequest = {}): Promise<MemoryTrackerActionResult> => {
    const trackers = activeUnderstandingTrackers(
      noteRecallInfo.value,
      propertyKey
    )
    if (trackers.length === 0) {
      return notCompleted
    }

    const confirmed = await popups.confirm(REMOVE_FROM_RECALL_CONFIRM)
    if (!confirmed) {
      return notCompleted
    }

    const success = await removeMemoryTrackersFromRecall(trackers)
    if (!success) {
      return notCompleted
    }

    await reloadNoteInfo()
    return { completed: true, navigated: false }
  }

  return {
    assimilatingPropertyKey,
    showSpellingPopup,
    assimilate,
    skip,
    revive,
    returnToSequence,
    removeFromRecall,
    handleSpellingVerified,
    handleSpellingCancel,
  }
}
