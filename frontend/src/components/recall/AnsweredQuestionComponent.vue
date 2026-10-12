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
        v-if="canRefineNote"
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
    v-if="answeredQuestion.mcq"
    v-bind="{
      questionStem: answeredQuestion.mcq.questionStem,
      responseChoices: answeredQuestion.mcq.responseChoices,
      correctChoiceIndex: answeredQuestion.mcq.correctAnswerIndex,
      answer: answeredQuestion.answer,
      testedFocus: answeredQuestion.mcq.testedFocus,
      validationRationale: answeredQuestion.mcq.validationRationale,
    }"
  />
  <ConversationButton
    v-if="conversationButton"
    :recall-prompt-id="answeredQuestion.id"
  />
  <RefineNoteModal
    v-if="canRefineNote && note"
    v-model:open="showRefineNoteModal"
    :note="note"
    :question-context="questionContext"
    :review-clarification="reviewClarification"
  />
</template>

<script setup lang="ts">
import type {
  AnsweredQuestion,
  Note,
  NoteRealm,
  NoteRefinementQuestionContextDto,
  User,
} from "@generated/donut-backend-api"
import type { PropType, Ref } from "vue"
import { computed, inject, ref } from "vue"
import QuestionDisplay from "./QuestionDisplay.vue"
import ConversationButton from "./ConversationButton.vue"
import NoteUnderQuestion from "./NoteUnderQuestion.vue"
import ViewMemoryTrackerLink from "./ViewMemoryTrackerLink.vue"
import RefineNoteModal from "./RefineNoteModal.vue"
import { recalledNoteUnderQuestionProps } from "./recalledNoteUnderQuestionProps"
import { useNoteStore } from "@/store/noteStore"
import { hasNoteContent } from "@/utils/hasNoteContent"

const props = defineProps({
  answeredQuestion: {
    type: Object as PropType<AnsweredQuestion>,
    required: true,
  },
  conversationButton: {
    type: Boolean,
    required: true,
  },
  reviewClarification: Boolean,
})

const noteStore = useNoteStore()
const currentUser = inject<Ref<User | undefined>>("currentUser")
const showRefineNoteModal = ref(false)

const noteRealm = computed<NoteRealm | undefined>(() => {
  const recalled = props.answeredQuestion.recalledNote
  if (!recalled) return undefined
  return noteStore.getNoteRealmRefAndLoadWhenNeeded(recalled.noteTopology.id)
    .value
})

const note = computed<Note | undefined>(() => noteRealm.value?.note)
const canRefineNote = computed(
  () =>
    Boolean(currentUser?.value) &&
    noteRealm.value?.notebookRealm.readonly !== true &&
    hasNoteContent(note.value?.content)
)

const questionContext = computed<NoteRefinementQuestionContextDto | undefined>(
  () => {
    const mcq = props.answeredQuestion.mcq
    if (!mcq) return undefined
    return {
      stem: mcq.questionStem,
      choices: mcq.responseChoices,
      correctAnswerIndex: mcq.correctAnswerIndex,
      selectedChoiceIndex: props.answeredQuestion.answer?.choiceIndex,
      testedFocus: mcq.testedFocus,
    }
  }
)
</script>
