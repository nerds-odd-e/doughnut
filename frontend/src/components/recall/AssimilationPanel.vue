<template>
  <AssimilationSettings
    ref="settingsRef"
    :note="note"
    :note-info-loaded="noteInfoLoaded"
    :assimilate-disabled="assimilateDisabled"
    :assimilating-property-key="assimilatingPropertyKey"
    @level-changed="emit('reloadNeeded')"
    @note-recall-info-loaded="onNoteRecallInfoLoaded"
    @assimilate="processAssimilate"
    @skip="processSkip"
    @revive="processRevive"
    @return-to-sequence="processReturnToSequence"
    @remove-from-recall="processRemoveFromRecall"
    @refinement-content-updated="emit('reloadNeeded')"
  />
  <Teleport to="body">
    <div
      v-if="showSpellingPopup"
      data-test="opaque-content-blocker"
      class="fixed inset-0 bg-black"
      style="z-index: 9989"
      aria-hidden="true"
    />
  </Teleport>
  <SpellingVerificationPopup
    :show="showSpellingPopup"
    :note-id="note.id"
    @cancel="handleSpellingCancel"
    @verified="handleSpellingVerified"
  />
</template>

<script setup lang="ts">
import type { Note, NoteRecallInfo } from "@generated/doughnut-backend-api"
import usePopups from "../commons/Popups/usePopups"
import AssimilationSettings from "./AssimilationSettings.vue"
import SpellingVerificationPopup from "./SpellingVerificationPopup.vue"
import { computed, ref } from "vue"
import {
  useAssimilateUnit,
  type AssimilateEvent,
} from "@/composables/useAssimilateUnit"
import {
  SEQUENCE_SKIP_CONFIRM,
  useAssimilationSequenceSkip,
} from "@/composables/useAssimilationSequenceSkip"
import {
  hasUnderstandingNoteLevelTracker,
  activeUnderstandingNoteLevelTrackers,
} from "./noteLevelMemoryTrackers"
import {
  trackersToRevive,
  useReviveMemoryTracker,
} from "@/composables/useReviveMemoryTracker"
import {
  REMOVE_FROM_RECALL_CONFIRM,
  useRemoveFromRecall,
} from "@/composables/useRemoveFromRecall"

const { note } = defineProps<{
  note: Note
}>()

const emit = defineEmits<{
  (e: "reloadNeeded"): void
}>()

const { popups } = usePopups()
const { assimilateUnit } = useAssimilateUnit()
const { skipFromAssimilationSequence, returnToAssimilationSequence } =
  useAssimilationSequenceSkip()
const { reviveMemoryTrackers } = useReviveMemoryTracker()
const { removeMemoryTrackersFromRecall } = useRemoveFromRecall()

const settingsRef = ref<InstanceType<typeof AssimilationSettings> | null>(null)

const pendingAssimilateAfterSpelling = ref<AssimilateEvent | null>(null)
const showSpellingPopup = computed(
  () => pendingAssimilateAfterSpelling.value !== null
)
const assimilatingPropertyKey = ref<string | null>(null)

const noteInfoLoaded = ref(false)
const noteRecallInfo = ref<NoteRecallInfo | null>(null)

const onNoteRecallInfoLoaded = (info: NoteRecallInfo) => {
  noteRecallInfo.value = info
  noteInfoLoaded.value = true
}

const assimilateDisabled = computed(() =>
  hasUnderstandingNoteLevelTracker(noteRecallInfo.value?.memoryTrackers)
)

const processAssimilate = async ({
  propertyKey,
  assimilateAsCommissioned,
  assimilateAsSpelling,
}: AssimilateEvent) => {
  if (assimilateAsCommissioned) {
    await doAssimilate({
      skipMemoryTracking: false,
      assimilateAsCommissioned,
    })
    return
  }

  if (assimilateAsSpelling) {
    pendingAssimilateAfterSpelling.value = {
      skipMemoryTracking: false,
      assimilateAsSpelling: true,
    }
    return
  }

  await doAssimilate({ skipMemoryTracking: false, propertyKey })
}

const processSkip = async ({ propertyKey }: { propertyKey?: string } = {}) => {
  const confirmed = await popups.confirm(SEQUENCE_SKIP_CONFIRM)
  if (!confirmed) {
    return
  }

  const result = await skipFromAssimilationSequence(note.id, propertyKey)
  if (!result.success) {
    return
  }

  await settingsRef.value?.reloadNoteInfo()

  if (!result.navigated) {
    emit("reloadNeeded")
  }
}

const processRevive = async ({ propertyKey }: { propertyKey?: string }) => {
  const trackers = trackersToRevive(noteRecallInfo.value, propertyKey)
  if (trackers.length === 0) {
    return
  }

  const success = await reviveMemoryTrackers(trackers)
  if (success) {
    await settingsRef.value?.reloadNoteInfo()
  }
}

const processReturnToSequence = async () => {
  const success = await returnToAssimilationSequence(note.id)
  if (success) {
    await settingsRef.value?.reloadNoteInfo()
  }
}

const processRemoveFromRecall = async () => {
  const trackers = activeUnderstandingNoteLevelTrackers(
    noteRecallInfo.value?.memoryTrackers
  )
  if (trackers.length === 0) {
    return
  }

  const confirmed = await popups.confirm(REMOVE_FROM_RECALL_CONFIRM)
  if (!confirmed) {
    return
  }

  const success = await removeMemoryTrackersFromRecall(trackers)
  if (success) {
    await settingsRef.value?.reloadNoteInfo()
  }
}

const doAssimilate = async ({
  skipMemoryTracking,
  propertyKey,
  assimilateAsCommissioned,
  assimilateAsSpelling,
}: AssimilateEvent) => {
  assimilatingPropertyKey.value = propertyKey ?? null
  try {
    const result = await assimilateUnit({
      noteId: note.id,
      skipMemoryTracking,
      propertyKey,
      assimilateAsCommissioned,
      assimilateAsSpelling,
    })

    if (!result.success) {
      return
    }

    await settingsRef.value?.reloadNoteInfo()

    if (!result.navigated) {
      emit("reloadNeeded")
    }
  } finally {
    assimilatingPropertyKey.value = null
  }
}

const handleSpellingVerified = async () => {
  const pending = pendingAssimilateAfterSpelling.value
  pendingAssimilateAfterSpelling.value = null
  await doAssimilate(pending!)
}

const handleSpellingCancel = () => {
  pendingAssimilateAfterSpelling.value = null
}
</script>
