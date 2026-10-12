<template>
  <Teleport to="body">
    <dialog
      ref="dialogRef"
      class="daisy-modal"
      :class="{ 'daisy-modal-open': open }"
      data-test="refine-note-modal"
      @close="close"
    >
      <div class="daisy-modal-box max-w-4xl max-h-[90vh] overflow-y-auto">
        <h3 v-if="open" class="font-bold text-lg mb-3">Refine note</h3>
        <div v-if="open && reviewClarification && !refinementStarted">
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
          v-if="open && (!reviewClarification || refinementStarted)"
          :key="note.id"
          :note="note"
          :question-context="refinementQuestionContext"
          @content-updated="emit('contentUpdated')"
          @extracted="close"
        />
        <div v-if="open" class="daisy-modal-action mt-4">
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
      <form method="dialog" class="daisy-modal-backdrop">
        <button type="button" @click="close">close</button>
      </form>
    </dialog>
  </Teleport>
</template>

<script setup lang="ts">
import type {
  Note,
  NoteRefinementQuestionContextDto,
} from "@generated/donut-backend-api"
import { useDaisyDialog } from "@/composables/useDaisyDialog"
import { computed, ref, toRef, watch } from "vue"
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

const dialogRef = ref<HTMLDialogElement | null>(null)
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
useDaisyDialog(toRef(props, "open"), dialogRef)

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
