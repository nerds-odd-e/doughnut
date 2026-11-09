import { computed, ref, watch, type ComputedRef, type Ref } from "vue"
import usePopups from "@/components/commons/Popups/usePopups"
import {
  useAssimilateUnit,
  type AssimilateEvent,
} from "@/composables/useAssimilateUnit"
import {
  sequenceSkipConfirmation,
  useAssimilationSequenceSkip,
} from "@/composables/useAssimilationSequenceSkip"

export type MemoryTrackerActionRequest = { propertyKey?: string }

export type MemoryTrackerActionHandlers = {
  assimilatingPropertyKey: Ref<string | null>
  understandingAdded: Ref<boolean>
  showSpellingPopup: ComputedRef<boolean>
  assimilate: (event: AssimilateEvent) => Promise<void>
  skip: (request?: MemoryTrackerActionRequest) => Promise<void>
  returnToSequence: (request?: MemoryTrackerActionRequest) => Promise<void>
  handleSpellingVerified: () => Promise<void>
  handleSpellingCancel: () => void
}

/**
 * Builds the memory-tracker action handlers (assimilate incl. the
 * spelling-verification flow, skip, return-to-sequence) for a single note.
 * `reloadNoteInfo` refreshes the shared `noteRecallInfo` owned by the caller
 * after each successful action. `understandingAdded` is this visit's
 * Understanding result: it lasts while the note stays the same, through
 * optional-mode additions, and resets when the note changes.
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
  const understandingAdded = ref(false)
  watch(noteId, () => {
    understandingAdded.value = false
  })
  const pendingAssimilateAfterSpelling = ref<AssimilateEvent | null>(null)
  const showSpellingPopup = computed(
    () => pendingAssimilateAfterSpelling.value !== null
  )

  const doAssimilate = async (event: AssimilateEvent) => {
    assimilatingPropertyKey.value = event.propertyKey ?? null
    try {
      const result = await assimilateUnit({ noteId: noteId.value, ...event })
      if (!result.success) {
        return
      }
      await reloadNoteInfo()
      if (result.understandingAdded) {
        understandingAdded.value = true
      }
    } finally {
      assimilatingPropertyKey.value = null
    }
  }

  const assimilate = async (event: AssimilateEvent) => {
    if (event.assimilateAsSpelling) {
      pendingAssimilateAfterSpelling.value = event
      return
    }
    await doAssimilate(event)
  }

  const handleSpellingVerified = async () => {
    const pending = pendingAssimilateAfterSpelling.value
    pendingAssimilateAfterSpelling.value = null
    if (!pending) {
      return
    }
    await doAssimilate(pending)
  }

  const handleSpellingCancel = () => {
    pendingAssimilateAfterSpelling.value = null
  }

  const skip = async ({ propertyKey }: MemoryTrackerActionRequest = {}) => {
    const confirmed = await popups.confirm(
      sequenceSkipConfirmation(propertyKey)
    )
    if (!confirmed) {
      return
    }

    const result = await skipFromAssimilationSequence(noteId.value, propertyKey)
    if (!result.success) {
      return
    }

    await reloadNoteInfo()
  }

  const returnToSequence = async ({
    propertyKey,
  }: MemoryTrackerActionRequest = {}) => {
    const success = await returnToAssimilationSequence(
      noteId.value,
      propertyKey
    )
    if (!success) {
      return
    }

    await reloadNoteInfo()
  }

  return {
    assimilatingPropertyKey,
    understandingAdded,
    showSpellingPopup,
    assimilate,
    skip,
    returnToSequence,
    handleSpellingVerified,
    handleSpellingCancel,
  }
}
