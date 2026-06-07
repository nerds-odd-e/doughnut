<template>
  <div class="daisy-alert" :class="{ 'daisy-alert-success': answeredQuestion.answer.correct, 'daisy-alert-error': !answeredQuestion.answer.correct }">
    <strong>
      {{ answeredQuestion.answer.correct ? 'Correct!' : `Your answer \`${answeredQuestion.answer.spellingAnswer}\` is incorrect.` }}
    </strong>
  </div>
  <NoteUnderQuestion
    v-bind="{
      noteTopology: answeredQuestion.recalledNote.noteTopology,
      ancestorFolders: answeredQuestion.recalledNote.ancestorFolders ?? [],
      breadcrumbNotebookId: answeredQuestion.recalledNote.notebookId,
      focusedPropertyKey: answeredQuestion.recalledNote.propertyKey,
    }"
  />
  <ViewMemoryTrackerLink
    :memory-tracker-id="answeredQuestion.memoryTrackerId"
  />
  <NoteShow
    :note-id="answeredQuestion.recalledNote.noteTopology.id"
    :expand-children="false"
  />
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { AnsweredQuestion } from "@generated/doughnut-backend-api"
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
