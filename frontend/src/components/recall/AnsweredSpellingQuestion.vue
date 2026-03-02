<template>
  <div class="daisy-alert" :class="{ 'daisy-alert-success': result.answer?.correct, 'daisy-alert-error': !result.answer?.correct }">
    <strong>
      {{ result.answer?.correct ? 'Correct!' : `Your answer \`${result.answer?.spellingAnswer}\` is incorrect.` }}
    </strong>
  </div>
  <NoteUnderQuestion v-if="result.note" v-bind="{ noteTopology: result.note.noteTopology }" />
  <ViewMemoryTrackerLink
    v-if="result.memoryTrackerId"
    :memory-tracker-id="result.memoryTrackerId"
  />
  <NoteShow
    v-if="result.note"
    :note-id="result.note.id"
    :expand-children="false"
  />
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { SpellingResult } from "@generated/backend"
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
