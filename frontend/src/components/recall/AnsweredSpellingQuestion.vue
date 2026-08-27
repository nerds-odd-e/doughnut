<template>
  <div
    class="daisy-alert"
    :class="alertClass"
    :data-testid="alertTestId"
  >
    <strong v-if="isAccidentalMatch">
      Your answer `<router-link
        v-if="uniqueAccidentalMatch"
        :to="noteShowLocation(uniqueAccidentalMatch.id)"
        class="daisy-link daisy-link-hover"
        data-testid="accidental-match-answer-link"
        >{{ spellingAnswer }}</router-link
      ><template v-else>{{ spellingAnswer }}</template>` names another note — not correct for this review.
    </strong>
    <strong v-else>{{ alertMessage }}</strong>
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
      :reviewed-note-id="answeredQuestion.recalledNote.noteTopology.id"
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
import type { AnsweredQuestion } from "@generated/donut-backend-api"
import NoteShow from "@/components/notes/NoteShow.vue"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import AccidentalMatchResolveDialog from "@/components/recall/AccidentalMatchResolveDialog.vue"
import { noteShowLocation } from "@/routes/noteShowLocation"
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

const isAccidentalMatch = computed(
  () => props.answeredQuestion.answer.outcome === "ACCIDENTAL_MATCH"
)

const spellingAnswer = computed(
  () => props.answeredQuestion.answer.spellingAnswer
)

const showResolveAccidentalMatchCta = computed(
  () =>
    isAccidentalMatch.value &&
    (props.answeredQuestion.matchedNotes?.length ?? 0) > 0
)

const uniqueAccidentalMatch = computed(() => {
  const notes = props.answeredQuestion.matchedNotes
  if (!isAccidentalMatch.value || notes?.length !== 1) return undefined
  return notes[0]
})

const alertClass = computed(() => {
  if (isOverlap.value) return "daisy-alert-warning"
  if (props.answeredQuestion.answer.correct) return "daisy-alert-success"
  return "daisy-alert-error"
})

const alertTestId = computed(() => {
  if (isOverlap.value) return "overlap-try-again-alert"
  if (isAccidentalMatch.value) return "accidental-match-alert"
  return undefined
})

const alertMessage = computed(() => {
  const { answer } = props.answeredQuestion
  if (isOverlap.value) {
    return "Correct, but we're looking for another answer — try again."
  }
  if (answer.correct) {
    return "Correct!"
  }
  return `Your answer \`${answer.spellingAnswer}\` is incorrect.`
})
</script>
