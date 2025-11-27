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
import { ConversationMessageController } from "@generated/backend/sdk.gen"
import { globalClientSilent } from "@/managedApi/clientSetup"

const props = defineProps<{
  conversationId: number
}>()

const emit = defineEmits<{
  (e: "close"): void
}>()

const exportContent = ref("")

onMounted(async () => {
  const { data: response, error } =
    await ConversationMessageController.exportConversation({
      path: { conversationId: props.conversationId },
      client: globalClientSilent,
    })
  if (!error) {
    exportContent.value = JSON.stringify(response, null, 2)
  } else {
    console.error("Failed to fetch export content:", error)
    exportContent.value = "Failed to load export content"
  }
})
</script>

