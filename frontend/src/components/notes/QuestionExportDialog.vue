<template>
  <Modal @close_request="$emit('close')">
    <template #body>
      <div class="daisy-card">
        <div class="daisy-card-body">
          <h3 class="daisy-card-title">Export Question Generation Request for ChatGPT</h3>
          <div class="daisy-mt-4">
            <textarea
              class="daisy-textarea daisy-textarea-bordered daisy-w-full daisy-h-96 daisy-bg-base-100 daisy-font-mono daisy-text-xs"
              readonly
              :value="exportContent"
              data-testid="export-textarea"
            />
            <div class="daisy-flex daisy-gap-2 daisy-justify-end daisy-mt-2">
              <CopyButton
                :text="exportContent"
                :disabled="!exportContent"
                test-id="copy-export-btn"
                aria-label="Copy to clipboard"
              />
            </div>
          </div>
        </div>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import Modal from "@/components/commons/Modal.vue"
import CopyButton from "@/components/commons/CopyButton.vue"
import { PredefinedQuestionController } from "@generated/backend/sdk.gen"

const props = defineProps<{
  noteId: number
}>()

const emit = defineEmits<{
  (e: "close"): void
}>()

const exportContent = ref("")

onMounted(async () => {
  const { data: response, error } =
    await PredefinedQuestionController.exportQuestionGeneration({
      path: { note: props.noteId },
    })
  if (!error && response) {
    exportContent.value = JSON.stringify(response, null, 2)
  } else {
    exportContent.value = "Failed to load export content"
  }
})
</script>

