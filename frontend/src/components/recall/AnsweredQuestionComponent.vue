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
  <Teleport to="body">
    <dialog
      v-if="hasNoteContent && note"
      ref="refineNoteDialogRef"
      class="daisy-modal"
      :class="{ 'daisy-modal-open': showRefineNoteModal }"
      data-test="refine-note-modal"
      @close="closeRefineNoteModal"
    >
      <div
        class="daisy-modal-box max-w-4xl max-h-[90vh] overflow-y-auto"
      >
        <h3
          v-if="showRefineNoteModal"
          class="font-bold text-lg mb-3"
        >
          Refine note
        </h3>
        <NoteRefinement
          v-if="showRefineNoteModal"
          :key="note.id"
          :note="note"
        />
        <div v-if="showRefineNoteModal" class="daisy-modal-action mt-4">
          <button
            type="button"
            class="daisy-btn"
            data-test="close-refine-note-modal"
            @click="closeRefineNoteModal"
          >
            Close
          </button>
        </div>
      </div>
      <form method="dialog" class="daisy-modal-backdrop">
        <button type="button" @click="closeRefineNoteModal">close</button>
      </form>
    </dialog>
  </Teleport>
</template>

<script setup lang="ts">
import type { AnsweredQuestion, Note } from "@generated/doughnut-backend-api"
import type { PropType } from "vue"
import { computed, ref, watch } from "vue"
import QuestionDisplay from "./QuestionDisplay.vue"
import ConversationButton from "./ConversationButton.vue"
import NoteUnderQuestion from "./NoteUnderQuestion.vue"
import ViewMemoryTrackerLink from "./ViewMemoryTrackerLink.vue"
import NoteRefinement from "./NoteRefinement.vue"
import { recalledNoteUnderQuestionProps } from "./recalledNoteUnderQuestionProps"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { useDaisyDialog } from "@/composables/useDaisyDialog"

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
const refineNoteDialogRef = ref<HTMLDialogElement | null>(null)
useDaisyDialog(showRefineNoteModal, refineNoteDialogRef)

const note = computed<Note | undefined>(() => {
  const recalled = props.answeredQuestion.recalledNote
  if (!recalled) return undefined
  return storageAccessor.value
    .storedApi()
    .getNoteRealmRefAndLoadWhenNeeded(recalled.noteTopology.id).value?.note
})

const hasNoteContent = computed(() => !!(note.value?.content ?? "").trim())

watch(
  () => note.value?.id,
  () => {
    showRefineNoteModal.value = false
  }
)

const closeRefineNoteModal = () => {
  showRefineNoteModal.value = false
}
</script>
