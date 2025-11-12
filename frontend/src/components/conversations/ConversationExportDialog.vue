<template>
  <Modal @close_request="$emit('close')">
    <template #body>
      <div class="daisy-card">
        <div class="daisy-card-body">
          <h3 class="daisy-card-title">Export Conversation for ChatGPT</h3>
          <div class="daisy-mt-4">
            <textarea
              class="daisy-textarea daisy-textarea-bordered daisy-w-full daisy-h-96 daisy-bg-base-100 daisy-font-mono daisy-text-xs"
              readonly
              :value="exportContent"
              data-testid="export-textarea"
            />
            <div class="daisy-flex daisy-gap-2 daisy-justify-end daisy-mt-2">
              <button
                class="daisy-btn daisy-btn-secondary daisy-btn-circle"
                @click="copyToClipboard"
                :disabled="!exportContent"
                data-testid="copy-export-btn"
                aria-label="Copy to clipboard"
              >
                <SvgClipboard v-if="!copied" />
                <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="none" viewBox="0 0 24 24"><path stroke="currentColor" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
              </button>
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
import SvgClipboard from "@/components/svgs/SvgClipboard.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"

const props = defineProps<{
  conversationId: number
}>()

const emit = defineEmits<{
  (e: "close"): void
}>()

const { managedApi } = useLoadingApi()
const exportContent = ref("")
const copied = ref(false)

onMounted(async () => {
  try {
    exportContent.value =
      await managedApi.restConversationMessageController.exportConversation(
        props.conversationId
      )
  } catch (error) {
    console.error("Failed to fetch export content:", error)
    exportContent.value = "Failed to load export content"
  }
})

async function copyToClipboard() {
  if (!exportContent.value) return
  await navigator.clipboard.writeText(exportContent.value)
  copied.value = true
  setTimeout(() => {
    copied.value = false
  }, 1200)
}
</script>

