<template>
  <div v-if="answeredQuestion.recalledNote">
    <NoteUnderQuestion
      v-bind="recalledNoteUnderQuestionProps(answeredQuestion.recalledNote)"
    />
    <div class="flex flex-wrap items-center gap-2">
      <ViewMemoryTrackerLink
        :memory-tracker-id="answeredQuestion.memoryTrackerId"
      />
      <button
        v-if="hasNoteContent"
        type="button"
        data-test="open-refine-note-modal"
        class="daisy-btn daisy-btn-neutral mt-4"
        @click="showRefineNoteModal = true"
      >
        Refine note
      </button>
    </div>
  </div>
  <QuestionDisplay
    v-if="answeredQuestion.predefinedQuestion"
    v-bind="{
      multipleChoicesQuestion: answeredQuestion.predefinedQuestion.multipleChoicesQuestion,
      correctChoiceIndex: answeredQuestion.predefinedQuestion.correctAnswerIndex,
      answer: answeredQuestion.answer,
      testedFocus: answeredQuestion.predefinedQuestion.testedFocus,
      validationRationale: answeredQuestion.predefinedQuestion.validationRationale,
    }"
  />
  <ConversationButton
    v-if="conversationButton"
    :recall-prompt-id="answeredQuestion.id"
  />
  <RefineNoteModal
    v-if="hasNoteContent && note"
    v-model:open="showRefineNoteModal"
    :note="note"
  />
</template>

<script setup lang="ts">
import type { AnsweredQuestion, Note } from "@generated/doughnut-backend-api"
import type { PropType } from "vue"
import { computed, ref } from "vue"
import QuestionDisplay from "./QuestionDisplay.vue"
import ConversationButton from "./ConversationButton.vue"
import NoteUnderQuestion from "./NoteUnderQuestion.vue"
import ViewMemoryTrackerLink from "./ViewMemoryTrackerLink.vue"
import RefineNoteModal from "./RefineNoteModal.vue"
import { recalledNoteUnderQuestionProps } from "./recalledNoteUnderQuestionProps"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const props = defineProps({
  answeredQuestion: {
    type: Object as PropType<AnsweredQuestion>,
    required: true,
  },
  conversationButton: {
    type: Boolean,
    required: true,
  },
})

const storageAccessor = useStorageAccessor()
const showRefineNoteModal = ref(false)

const note = computed<Note | undefined>(() => {
  const recalled = props.answeredQuestion.recalledNote
  if (!recalled) return undefined
  return storageAccessor.value
    .storedApi()
    .getNoteRealmRefAndLoadWhenNeeded(recalled.noteTopology.id).value?.note
})

const hasNoteContent = computed(() => !!(note.value?.content ?? "").trim())
</script>
