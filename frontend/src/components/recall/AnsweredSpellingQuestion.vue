<template>
  <div
    class="daisy-alert"
    :class="{
      'daisy-alert-success': answeredQuestion.answer.correct,
      'daisy-alert-error': !answeredQuestion.answer.correct,
    }"
    :data-testid="
      answeredQuestion.answer.outcome === 'ACCIDENTAL_MATCH'
        ? 'accidental-match-alert'
        : undefined
    "
  >
    <strong>{{ alertMessage }}</strong>
  </div>
  <NoteUnderQuestion
    v-bind="recalledNoteUnderQuestionProps(answeredQuestion.recalledNote)"
  />
  <ViewMemoryTrackerLink
    :memory-tracker-id="answeredQuestion.memoryTrackerId"
  />
  <NoteShow
    :note-id="answeredQuestion.recalledNote.noteTopology.id"
    :expand-children="false"
  />
  <section
    v-if="showMatchedNotesSection"
    class="mt-6"
    data-testid="matched-notes-section"
  >
    <h4 class="text-lg font-semibold mb-4 text-base-content">Matched note(s)</h4>
    <div class="flex flex-col gap-4">
      <div
        v-for="matched in answeredQuestion.matchedNotes"
        :key="matched.id"
        :data-testid="`matched-note-${matched.id}`"
      >
        <NoteShow :note-id="matched.id" :expand-children="false" />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, type PropType } from "vue"
import type { AnsweredQuestion } from "@generated/doughnut-backend-api"
import NoteShow from "@/components/notes/NoteShow.vue"
import NoteUnderQuestion from "./NoteUnderQuestion.vue"
import ViewMemoryTrackerLink from "./ViewMemoryTrackerLink.vue"
import { recalledNoteUnderQuestionProps } from "./recalledNoteUnderQuestionProps"

const props = defineProps({
  answeredQuestion: {
    type: Object as PropType<AnsweredQuestion>,
    required: true,
  },
})

const showMatchedNotesSection = computed(
  () =>
    props.answeredQuestion.answer.outcome === "ACCIDENTAL_MATCH" &&
    (props.answeredQuestion.matchedNotes?.length ?? 0) > 0
)

const alertMessage = computed(() => {
  const { answer } = props.answeredQuestion
  if (answer.outcome === "ACCIDENTAL_MATCH") {
    return `Your answer \`${answer.spellingAnswer}\` names another note — not correct for this review.`
  }
  if (answer.correct) {
    return "Correct!"
  }
  return `Your answer \`${answer.spellingAnswer}\` is incorrect.`
})
</script>
