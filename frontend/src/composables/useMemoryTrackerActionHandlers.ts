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
  returnToSequence: (
    request?: MemoryTrackerActionRequest
  ) => Promise<MemoryTrackerActionResult>
  handleSpellingVerified: () => Promise<MemoryTrackerActionResult>
  handleSpellingCancel: () => void
}

/**
 * Builds the memory-tracker action handlers (assimilate incl. the
 * spelling-verification flow, skip, return-to-sequence) for a single note.
 * `reloadNoteInfo` refreshes the shared `noteRecallInfo` owned by the caller
 * after each successful action.
 */
export function useMemoryTrackerActionHandlers(
  noteId: Ref<number>,
  reloadNoteInfo: () => Promise<void>
): MemoryTrackerActionHandlers {
  const { popups } = usePopups()
  const { assimilateUnit } = useAssimilateUnit()
  const { skipFromAssimilationSequence, returnToAssimilationSequence } =
    useAssimilationSequenceSkip()

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

  return {
    assimilatingPropertyKey,
    showSpellingPopup,
    assimilate,
    skip,
    returnToSequence,
    handleSpellingVerified,
    handleSpellingCancel,
  }
}
