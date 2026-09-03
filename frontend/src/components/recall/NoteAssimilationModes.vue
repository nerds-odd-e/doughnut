<template>
  <section
    aria-label="Assimilation modes"
    data-testid="note-assimilation-modes"
    class="flex flex-col items-start gap-2"
  >
    <h2 class="text-sm font-semibold">Recall modes</h2>
    <AssimilationModes
      :allowed-modes="allowedModes"
      :trackers="noteRecallInfo?.memoryTrackers"
      :disabled="!noteInfoLoaded"
      :skipped-from-assimilation-sequence="
        isSkippedFromAssimilationSequence(noteRecallInfo)
      "
      @assimilate="emit('assimilate', $event)"
      @skip="emit('skip', {})"
      @return-to-sequence="emit('returnToSequence', {})"
    />
  </section>
</template>

<script setup lang="ts">
import type { Note } from "@generated/donut-backend-api"
import AssimilationModes from "./AssimilationModes.vue"
import type { AssimilateEvent } from "@/composables/useAssimilateUnit"
import { isSkippedFromAssimilationSequence } from "@/composables/useAssimilationSequenceSkip"
import { useInjectedMemoryTrackerActions } from "@/composables/useMemoryTrackerActions"
import { toRef } from "vue"
import type { MemoryTrackerType } from "./assimilationMemoryTrackers"

const { note, noteInfoLoaded } = defineProps<{
  note: Note
  noteInfoLoaded: boolean
}>()

const emit = defineEmits<{
  (e: "assimilate", request: AssimilateEvent): void
  (e: "skip", request: { propertyKey?: string }): void
  (e: "returnToSequence", request: { propertyKey?: string }): void
}>()

const { noteRecallInfo } = useInjectedMemoryTrackerActions(toRef(() => note.id))

const allowedModes: MemoryTrackerType[] = [
  "COMMISSIONED",
  "SPELLING",
  "UNDERSTANDING",
]
</script>
