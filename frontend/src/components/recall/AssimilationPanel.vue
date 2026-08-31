<template>
  <AssimilationSettings
    :note="note"
    :note-info-loaded="noteInfoLoaded"
    :assimilate-disabled="assimilateDisabled"
    @assimilate="onAssimilate"
    @skip="onSkip"
    @revive="onRevive"
    @return-to-sequence="onReturnToSequence"
    @remove-from-recall="onRemoveFromRecall"
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
    @verified="onSpellingVerified"
  />
</template>

<script setup lang="ts">
import type { Note } from "@generated/donut-backend-api"
import AssimilationSettings from "./AssimilationSettings.vue"
import SpellingVerificationPopup from "./SpellingVerificationPopup.vue"
import { provide, toRef } from "vue"
import type { AssimilateEvent } from "@/composables/useAssimilateUnit"
import {
  memoryTrackerActionsKey,
  useInjectedMemoryTrackerActions,
  type MemoryTrackerActionRequest,
  type MemoryTrackerActionResult,
} from "@/composables/useMemoryTrackerActions"

const { note } = defineProps<{
  note: Note
}>()

const emit = defineEmits<{
  (e: "reloadNeeded"): void
}>()

// Re-provide the resolved instance so `AssimilationSettings.vue` (our own
// child) always shares it, even in isolated mounts (tests) that render
// this panel without `NoteShow.vue`'s ancestor provider.
const memoryTrackerActions = useInjectedMemoryTrackerActions(
  toRef(() => note.id)
)
provide(memoryTrackerActionsKey, memoryTrackerActions)

const {
  noteInfoLoaded,
  assimilateDisabled,
  showSpellingPopup,
  handleSpellingCancel,
} = memoryTrackerActions

const afterAction = (
  result: MemoryTrackerActionResult,
  { emitReloadOnStay = false }: { emitReloadOnStay?: boolean } = {}
) => {
  if (!result.completed) {
    return
  }

  if (emitReloadOnStay && !result.navigated) {
    emit("reloadNeeded")
  }
}

const onAssimilate = async (event: AssimilateEvent) => {
  const result = await memoryTrackerActions.assimilate(event)
  afterAction(result, { emitReloadOnStay: true })
}

const onSpellingVerified = async () => {
  const result = await memoryTrackerActions.handleSpellingVerified()
  afterAction(result, { emitReloadOnStay: true })
}

const onSkip = async (request: MemoryTrackerActionRequest = {}) => {
  const result = await memoryTrackerActions.skip(request)
  afterAction(result, { emitReloadOnStay: true })
}

const onRevive = async (request: MemoryTrackerActionRequest) => {
  const result = await memoryTrackerActions.revive(request)
  afterAction(result)
}

const onReturnToSequence = async (request: MemoryTrackerActionRequest = {}) => {
  const result = await memoryTrackerActions.returnToSequence(request)
  afterAction(result)
}

const onRemoveFromRecall = async (request: MemoryTrackerActionRequest = {}) => {
  const result = await memoryTrackerActions.removeFromRecall(request)
  afterAction(result)
}
</script>
