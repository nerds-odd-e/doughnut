<template>
  <Modal @close_request="$emit('close')">
    <template #body>
      <div class="daisy-card">
        <div class="daisy-card-body">
          <h3 class="daisy-card-title">{{ title }}</h3>
          <div class="mt-4">
            <div
              v-if="isLoading"
              class="flex justify-center items-center h-96"
              data-testid="export-loading"
            >
              <span
                class="daisy-loading daisy-loading-spinner daisy-loading-lg"
              />
            </div>
            <template v-else>
              <textarea
                class="daisy-textarea w-full h-96 bg-base-100 font-mono text-xs"
                readonly
                :value="displayContent"
                data-testid="export-textarea"
              />
              <div class="flex gap-2 justify-end mt-2">
                <CopyButton
                  :text="displayContent"
                  :disabled="!displayContent"
                  test-id="copy-export-btn"
                  aria-label="Copy to clipboard"
                />
              </div>
            </template>
          </div>
        </div>
      </div>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import Modal from "@/components/commons/Modal.vue"
import CopyButton from "@/components/commons/CopyButton.vue"

const props = defineProps<{
  title: string
  exportContent?: string
  fetchExport?: () => Promise<unknown>
}>()

defineEmits<{
  (e: "close"): void
}>()

const loadedContent = ref("")
const isLoading = ref(false)

const displayContent = computed(() => {
  if (props.exportContent !== undefined) {
    return props.exportContent
  }
  return loadedContent.value
})

function formatExportContent(result: unknown): string {
  if (result === null || result === undefined) {
    return "Failed to load export content"
  }
  if (typeof result === "string") {
    return result
  }
  return JSON.stringify(result, null, 2)
}

onMounted(async () => {
  if (props.exportContent !== undefined || !props.fetchExport) {
    return
  }
  isLoading.value = true
  try {
    loadedContent.value = formatExportContent(await props.fetchExport())
  } catch {
    loadedContent.value = "Failed to load export content"
  } finally {
    isLoading.value = false
  }
})
</script>
