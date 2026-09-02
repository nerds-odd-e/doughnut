<template>
  <section
    aria-label="Assimilation settings"
    data-testid="assimilation-settings"
    class="flex flex-col gap-0"
  >
    <div
      class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"
    >
      <div class="flex flex-wrap items-stretch justify-end gap-2 sm:flex-1">
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
      </div>
    </div>
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
