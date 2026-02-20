<template>
  <Modal
    v-if="show"
    class="popups"
    :isPopup="true"
    :opaqueBackground="true"
    @close_request="$emit('cancel')"
  >
    <template #header>
      <h2 data-test="spelling-verification-popup">Verify Spelling</h2>
    </template>
    <template #body>
      <p class="daisy-mb-4">Please type the note title to verify your spelling:</p>
      <input
        v-model="userInput"
        type="text"
        class="daisy-input daisy-input-bordered daisy-w-full"
        data-test="spelling-verification-input"
        @keyup.enter="verify"
      />
      <p v-if="errorMessage" class="daisy-text-error daisy-mt-2" data-test="spelling-error-message">{{ errorMessage }}</p>
      <div class="daisy-mt-4 daisy-flex daisy-gap-2">
        <button class="daisy-btn daisy-btn-primary" data-test="verify-spelling" @click="verify">Verify</button>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { ref, watch } from "vue"
import Modal from "../commons/Modal.vue"

const props = defineProps<{
  show: boolean
  expectedTitle: string
}>()

const emit = defineEmits<{
  (e: "cancel"): void
  (e: "verified"): void
}>()

const userInput = ref("")
const errorMessage = ref("")

// Reset state when popup opens
watch(
  () => props.show,
  (newShow) => {
    if (newShow) {
      userInput.value = ""
      errorMessage.value = ""
    }
  }
)

const verify = () => {
  // Split the expected title by "/" to get all valid answers
  const validAnswers = props.expectedTitle
    .split("/")
    .map((part) => part.trim().toLowerCase())
  const userAnswer = userInput.value.trim().toLowerCase()

  if (validAnswers.includes(userAnswer)) {
    emit("verified")
  } else {
    errorMessage.value = "wrong spelling"
  }
}
</script>

