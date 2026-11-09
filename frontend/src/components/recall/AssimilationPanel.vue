<template>
  <NoteAssimilationModes
    :note="note"
    :note-info-loaded="noteInfoLoaded"
    :read-first="readFirst"
    @assimilate="memoryTrackerActions.assimilate"
    @skip="memoryTrackerActions.skip"
    @return-to-sequence="memoryTrackerActions.returnToSequence"
    @next="goToNextAssimilation"
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
    @verified="memoryTrackerActions.handleSpellingVerified"
  />
</template>

<script setup lang="ts">
import type { Note } from "@generated/donut-backend-api"
import NoteAssimilationModes from "./NoteAssimilationModes.vue"
import SpellingVerificationPopup from "./SpellingVerificationPopup.vue"
import { provide, toRef } from "vue"
import { useAssimilationView } from "@/composables/useAssimilationView"
import { useGoToNextAssimilation } from "@/composables/useGoToNextAssimilation"
import {
  memoryTrackerActionsKey,
  useInjectedMemoryTrackerActions,
} from "@/composables/useMemoryTrackerActions"

const { note, readFirst = false } = defineProps<{
  note: Note
  readFirst?: boolean
}>()

useAssimilationView().trackPresentation(() => note.id)
const { goToNextAssimilation } = useGoToNextAssimilation()

// Re-provide the resolved instance so `NoteAssimilationModes.vue` (our own
// child) always shares it, even in isolated mounts (tests) that render
// this panel without `NoteShow.vue`'s ancestor provider.
const memoryTrackerActions = useInjectedMemoryTrackerActions(
  toRef(() => note.id)
)
provide(memoryTrackerActionsKey, memoryTrackerActions)

const { noteInfoLoaded, showSpellingPopup, handleSpellingCancel } =
  memoryTrackerActions
</script>
