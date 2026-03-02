<template>
  <div class="daisy-alert" :class="{ 'daisy-alert-success': answeredQuestion.recallPrompt?.answer?.correct, 'daisy-alert-error': !answeredQuestion.recallPrompt?.answer?.correct }">
    <strong>
      {{ answeredQuestion.recallPrompt?.answer?.correct ? 'Correct!' : `Your answer \`${answeredQuestion.recallPrompt?.answer?.spellingAnswer}\` is incorrect.` }}
    </strong>
  </div>
  <NoteUnderQuestion v-if="answeredQuestion.recallPrompt?.note" v-bind="{ noteTopology: answeredQuestion.recallPrompt.note.noteTopology }" />
  <ViewMemoryTrackerLink
    v-if="answeredQuestion.recallPrompt?.memoryTrackerId"
    :memory-tracker-id="answeredQuestion.recallPrompt.memoryTrackerId"
  />
  <NoteShow
    v-if="answeredQuestion.recallPrompt?.note"
    :note-id="answeredQuestion.recallPrompt.note.id"
    :expand-children="false"
  />
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { AnsweredQuestion } from "@generated/backend"
import NoteShow from "@/components/notes/NoteShow.vue"
import NoteUnderQuestion from "./NoteUnderQuestion.vue"
import ViewMemoryTrackerLink from "./ViewMemoryTrackerLink.vue"

defineProps({
  answeredQuestion: {
    type: Object as PropType<AnsweredQuestion>,
    required: true,
  },
})
</script>
