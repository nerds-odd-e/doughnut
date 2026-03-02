<template>
  <div class="daisy-alert" :class="{ 'daisy-alert-success': answeredQuestion?.answer?.correct, 'daisy-alert-error': !answeredQuestion?.answer?.correct }">
    <strong>
      {{ answeredQuestion?.answer?.correct ? 'Correct!' : `Your answer \`${answeredQuestion?.answer?.spellingAnswer}\` is incorrect.` }}
    </strong>
  </div>
  <NoteUnderQuestion v-if="answeredQuestion?.note" v-bind="{ noteTopology: answeredQuestion.note.noteTopology }" />
  <ViewMemoryTrackerLink
    v-if="answeredQuestion?.memoryTrackerId"
    :memory-tracker-id="answeredQuestion.memoryTrackerId"
  />
  <NoteShow
    v-if="answeredQuestion?.note"
    :note-id="answeredQuestion.note.id"
    :expand-children="false"
  />
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { RecallPrompt } from "@generated/backend"
import NoteShow from "@/components/notes/NoteShow.vue"
import NoteUnderQuestion from "./NoteUnderQuestion.vue"
import ViewMemoryTrackerLink from "./ViewMemoryTrackerLink.vue"

defineProps({
  answeredQuestion: {
    type: Object as PropType<RecallPrompt>,
    required: true,
  },
})
</script>
