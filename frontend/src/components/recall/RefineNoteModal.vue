<template>
  <Modal v-if="open" :show-close-button="false" @close_request="close">
    <template #body>
      <div data-test="refine-note-modal">
        <h3 class="font-bold text-lg mb-3">Refine note</h3>
        <div v-if="reviewClarification && !refinementStarted">
          <label for="refinement-clarification" class="block mb-2">
            Clarification to carry into refinement
          </label>
          <textarea
            id="refinement-clarification"
            v-model="clarification"
            class="daisy-textarea daisy-textarea-bordered w-full"
            rows="4"
          />
          <button
            type="button"
            class="daisy-btn daisy-btn-primary mt-3"
            :disabled="!clarification.trim()"
            @click="continueToRefinement"
          >
            Continue to refinement
          </button>
        </div>
        <NoteRefinement
          v-if="!reviewClarification || refinementStarted"
          :key="note.id"
          :note="note"
          :question-context="refinementQuestionContext"
          @content-updated="emit('contentUpdated')"
          @extracted="close"
        />
        <div class="daisy-modal-action mt-4">
          <button
            type="button"
            class="daisy-btn"
            data-test="close-refine-note-modal"
            @click="close"
          >
            Close
          </button>
        </div>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import type {
  Note,
  NoteRefinementQuestionContextDto,
} from "@generated/donut-backend-api"
import Modal from "@/components/commons/Modal.vue"
import { computed, ref, watch } from "vue"
import NoteRefinement from "./NoteRefinement.vue"

const props = defineProps<{
  open: boolean
  note: Note
  questionContext?: NoteRefinementQuestionContextDto
  reviewClarification?: boolean
}>()

const emit = defineEmits<{
  "update:open": [value: boolean]
  contentUpdated: []
}>()

const clarification = ref("")
const carriedClarification = ref<string>()
const refinementStarted = computed(
  () => carriedClarification.value !== undefined
)
const refinementQuestionContext = computed(() =>
  props.reviewClarification
    ? { ...props.questionContext, clarification: carriedClarification.value }
    : props.questionContext
)

const continueToRefinement = () => {
  carriedClarification.value = clarification.value.trim()
}

watch(
  () => props.open,
  (open) => {
    if (open) {
      clarification.value = ""
      carriedClarification.value = undefined
    }
  }
)

watch(
  () => props.note.id,
  () => {
    emit("update:open", false)
  }
)

const close = () => {
  emit("update:open", false)
}
</script>
