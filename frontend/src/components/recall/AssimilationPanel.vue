<template>
  <AssimilationSettings
    ref="settingsRef"
    :note="note"
    :note-info-loaded="noteInfoLoaded"
    :keep-for-recall-disabled="keepForRecallDisabled"
    :assimilating-property-key="assimilatingPropertyKey"
    @level-changed="emit('reloadNeeded')"
    @remember-spelling-changed="onRememberSpellingChanged"
    @note-recall-info-loaded="onNoteRecallInfoLoaded"
    @assimilate="processAssimilate"
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
  skipRecallConfirmMessage,
  useAssimilateUnit,
  type AssimilateEvent,
} from "@/composables/useAssimilateUnit"
import { useAssimilationView } from "@/composables/useAssimilationView"

const { note } = defineProps<{
  note: Note
}>()

const emit = defineEmits<{
  (e: "reloadNeeded"): void
}>()

const { popups } = usePopups()
const { assimilateUnit } = useAssimilateUnit()
const { openForNote } = useAssimilationView()

const settingsRef = ref<InstanceType<typeof AssimilationSettings> | null>(null)

const showSpellingPopup = ref(false)
const assimilatingPropertyKey = ref<string | null>(null)

const rememberSpelling = ref(false)
const noteInfoLoaded = ref(false)
const noteRecallInfo = ref<NoteRecallInfo | null>(null)

const onRememberSpellingChanged = (value: boolean) => {
  rememberSpelling.value = value
}

const onNoteRecallInfoLoaded = (info: NoteRecallInfo) => {
  noteRecallInfo.value = info
  noteInfoLoaded.value = true
}

const hasNoteLevelMemoryTrackers = computed(
  () =>
    noteRecallInfo.value?.memoryTrackers?.some((mt) => !mt.propertyKey) ?? false
)
const hasSpellingMemoryTracker = computed(
  () =>
    noteRecallInfo.value?.memoryTrackers?.some((mt) => mt.spelling === true) ??
    false
)
const keepForRecallDisabled = computed(
  () =>
    hasNoteLevelMemoryTrackers.value &&
    !(rememberSpelling.value && !hasSpellingMemoryTracker.value)
)

const processAssimilate = async ({
  skipMemoryTracking,
  propertyKey,
}: AssimilateEvent) => {
  if (skipMemoryTracking) {
    const confirmed = await popups.confirm(
      skipRecallConfirmMessage(propertyKey)
    )
    if (!confirmed) {
      return
    }
  }

  if (!propertyKey && !skipMemoryTracking && rememberSpelling.value) {
    showSpellingPopup.value = true
    return
  }

  await doAssimilate({ skipMemoryTracking, propertyKey })
}

const doAssimilate = async ({
  skipMemoryTracking,
  propertyKey,
}: AssimilateEvent) => {
  assimilatingPropertyKey.value = propertyKey ?? null
  try {
    const result = await assimilateUnit({
      noteId: note.id,
      skipMemoryTracking,
      propertyKey,
    })

    if (!result.success) {
      return
    }

    if (propertyKey) {
      await settingsRef.value?.reloadNoteInfo()
    }

    if (!result.navigated) {
      if (propertyKey && skipMemoryTracking) {
        openForNote(note.id, null)
      } else {
        emit("reloadNeeded")
      }
    }
  } finally {
    assimilatingPropertyKey.value = null
  }
}

const handleSpellingVerified = async () => {
  showSpellingPopup.value = false
  await doAssimilate({ skipMemoryTracking: false })
}

const handleSpellingCancel = () => {
  showSpellingPopup.value = false
}
</script>
