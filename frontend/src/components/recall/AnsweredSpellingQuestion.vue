<template>
  <div class="daisy-alert" :class="{ 'daisy-alert-success': result.isCorrect, 'daisy-alert-error': !result.isCorrect }">
    <strong>
      {{ result.isCorrect ? 'Correct!' : `Your answer \`${result.answer}\` is incorrect.` }}
    </strong>
  </div>
  <NoteUnderQuestion v-bind="{ noteTopology: result.note.noteTopology }" />
  <ViewMemoryTrackerLink
    v-if="result.memoryTrackerId"
    :memory-tracker-id="result.memoryTrackerId"
  />
  <NoteShow
    :note-id="result.note.id"
    :expand-children="false"
  />
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { SpellingResult } from "@/pages/RecallPage.vue"
import NoteShow from "@/components/notes/NoteShow.vue"
import NoteUnderQuestion from "./NoteUnderQuestion.vue"
import ViewMemoryTrackerLink from "./ViewMemoryTrackerLink.vue"

defineProps({
  result: {
    type: Object as PropType<SpellingResult>,
    required: true,
  },
})
</script>
