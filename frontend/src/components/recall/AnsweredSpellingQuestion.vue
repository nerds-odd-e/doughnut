<template>
  <div
    class="daisy-alert"
    :class="alertClass"
    :data-testid="alertTestId"
  >
    <strong>{{ alertMessage }}</strong>
  </div>
  <PopButton
    v-if="showResolveAccidentalMatchCta"
    title="Resolve accidental match"
    aria-label="Resolve accidental match"
    btn-class="daisy-btn daisy-btn-secondary daisy-btn-sm mt-2"
    data-testid="resolve-accidental-match"
  >
    <AccidentalMatchResolveDialog
      :matched-notes="answeredQuestion.matchedNotes ?? []"
    />
  </PopButton>
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
  <button
    v-if="isOverlap"
    type="button"
    class="daisy-btn daisy-btn-secondary daisy-btn-sm mt-6"
    data-testid="overlap-try-again"
    title="Try again"
    aria-label="Try again"
    @click="emit('retry')"
  >
    Try again
  </button>
</template>

<script setup lang="ts">
import { computed, type PropType } from "vue"
import type { AnsweredQuestion } from "@generated/doughnut-backend-api"
import NoteShow from "@/components/notes/NoteShow.vue"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import AccidentalMatchResolveDialog from "@/components/recall/AccidentalMatchResolveDialog.vue"
import NoteUnderQuestion from "./NoteUnderQuestion.vue"
import ViewMemoryTrackerLink from "./ViewMemoryTrackerLink.vue"
import { recalledNoteUnderQuestionProps } from "./recalledNoteUnderQuestionProps"

const props = defineProps({
  answeredQuestion: {
    type: Object as PropType<AnsweredQuestion>,
    required: true,
  },
})

const emit = defineEmits<{
  (e: "retry"): void
}>()

const isOverlap = computed(
  () => props.answeredQuestion.answer.outcome === "OVERLAP"
)

const showResolveAccidentalMatchCta = computed(
  () =>
    props.answeredQuestion.answer.outcome === "ACCIDENTAL_MATCH" &&
    (props.answeredQuestion.matchedNotes?.length ?? 0) > 0
)

const alertClass = computed(() => {
  if (isOverlap.value) return "daisy-alert-warning"
  if (props.answeredQuestion.answer.correct) return "daisy-alert-success"
  return "daisy-alert-error"
})

const alertTestId = computed(() => {
  if (isOverlap.value) return "overlap-try-again-alert"
  if (props.answeredQuestion.answer.outcome === "ACCIDENTAL_MATCH") {
    return "accidental-match-alert"
  }
  return undefined
})

const alertMessage = computed(() => {
  const { answer } = props.answeredQuestion
  if (isOverlap.value) {
    return "Correct, but we're looking for another answer — try again."
  }
  if (answer.outcome === "ACCIDENTAL_MATCH") {
    return `Your answer \`${answer.spellingAnswer}\` names another note — not correct for this review.`
  }
  if (answer.correct) {
    return "Correct!"
  }
  return `Your answer \`${answer.spellingAnswer}\` is incorrect.`
})
</script>
