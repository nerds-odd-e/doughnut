<template>
  <div class="daisy-alert" :class="{ 'daisy-alert-success': result.isCorrect, 'daisy-alert-error': !result.isCorrect }">
    <strong>
      {{ result.isCorrect ? 'Correct!' : `Your answer \`${result.answer}\` is incorrect.` }}
    </strong>
  </div>
  <NoteUnderQuestion v-bind="{ noteTopology: result.note.noteTopology }" />
  <NoteShow
    :note-id="result.note.id"
    :storage-accessor="storageAccessor"
    :expand-children="false"
  />
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import type { SpellingResult } from "@/pages/RecallPageView.vue"
import type { StorageAccessor } from "@/store/createNoteStorage"
import NoteShow from "@/components/notes/NoteShow.vue"
import NoteUnderQuestion from "./NoteUnderQuestion.vue"

defineProps({
  result: {
    type: Object as PropType<SpellingResult>,
    required: true,
  },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
})
</script>
