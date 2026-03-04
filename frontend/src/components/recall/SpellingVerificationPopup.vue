<template>
  <Modal v-if="show" :isPopup="true" @close_request="$emit('cancel')">
    <template #header>
      <h2 data-test="spelling-verification-popup">Verify Spelling</h2>
    </template>
    <template #body>
      <p class="daisy-mb-4">Please type the note title from memory:</p>
      <input
        v-model="userInput"
        type="text"
        class="daisy-input daisy-input-bordered daisy-w-full"
        data-test="spelling-verification-input"
        @keyup.enter="verify"
      />
      <p v-if="errorMessage" class="daisy-text-error daisy-mt-2" data-test="spelling-error-message">{{ errorMessage }}</p>
      <div class="daisy-mt-4 daisy-flex daisy-gap-2">
        <button
          class="daisy-btn daisy-btn-primary"
          data-test="verify-spelling"
          :disabled="isVerifying"
          @click="verify"
        >
          Verify
        </button>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { ref, watch } from "vue"
import { NoteController } from "@generated/backend/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import Modal from "../commons/Modal.vue"

const props = defineProps<{
  show: boolean
  noteId: number
}>()

const emit = defineEmits<{
  (e: "cancel"): void
  (e: "verified"): void
}>()

const userInput = ref("")
const errorMessage = ref("")
const isVerifying = ref(false)

// Reset state when popup opens
watch(
  () => props.show,
  (newShow) => {
    if (newShow) {
      userInput.value = ""
      errorMessage.value = ""
      isVerifying.value = false
    }
  }
)

const verify = async () => {
  if (isVerifying.value) return
  isVerifying.value = true
  const { data, error } = await apiCallWithLoading(() =>
    NoteController.verifySpelling({
      path: { note: props.noteId },
      body: { spellingAnswer: userInput.value },
    })
  )

  if (error || !data?.correct) {
    errorMessage.value = "wrong spelling"
  } else {
    emit("verified")
  }
  isVerifying.value = false
}
</script>

