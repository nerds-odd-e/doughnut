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
import useLoadingApi from "@/managedApi/useLoadingApi"

const props = defineProps<{
  noteId: number
}>()

const emit = defineEmits<{
  (e: "close"): void
}>()

const { managedApi } = useLoadingApi()
const exportContent = ref("")

onMounted(async () => {
  try {
    const response = (await managedApi.services.exportQuestionGeneration({
      path: { note: props.noteId },
    })) as unknown
    exportContent.value = JSON.stringify(response, null, 2)
  } catch (error) {
    console.error("Failed to fetch export content:", error)
    exportContent.value = "Failed to load export content"
  }
})
</script>

