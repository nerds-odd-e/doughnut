<template>
  <div v-if="currentAiReply" class="daisy-flex daisy-mb-3">
    <div class="message-avatar daisy-me-2" title="AI Assistant">
      <SvgRobot />
    </div>
    <div class="daisy-card daisy-py-2 daisy-px-3 daisy-bg-light ai-chat"
    v-html="markdowntToHtml(currentAiReply)"
    />
  </div>

  <div v-if="currentSuggestion" class="daisy-flex daisy-mb-3">
    <div class="message-avatar daisy-me-2" title="AI Assistant">
      <SvgRobot />
    </div>
    <ToolCallHandler
      v-if="currentSuggestion"
      :suggestion="currentSuggestion"
      :note="currentNote"
      @resolved="handleToolCallResolved"
      @rejected="handleToolCallRejected"
    />
  </div>

  <div v-if="lastErrorMessage" class="last-error-message daisy-text-danger daisy-mb-3">
    {{ lastErrorMessage }}
  </div>

  <div v-if="aiStatus" class="daisy-flex daisy-align-items-center status-bar daisy-mb-3">
    <div class="daisy-spinner-border daisy-spinner-border-sm daisy-me-2" role="status">
      <span class="daisy-visually-hidden">Loading...</span>
    </div>
    <small class="daisy-text-secondary">{{ aiStatus }}</small>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from "vue"
import type { Conversation } from "@generated/backend"
import type { ToolCallResult } from "@/models/aiReplyState"
import SvgRobot from "@/components/svgs/SvgRobot.vue"
import markdownizer from "../form/markdownizer"
import {
  createAiReplyStates,
  type AiActionContext,
} from "@/models/aiReplyState"
import ToolCallHandler from "./ToolCallHandler.vue"
import { type Suggestion } from "@/models/suggestions"
import AiReplyEventSource from "@/managedApi/AiReplyEventSource"

const { conversation, aiReplyTrigger } = defineProps<{
  conversation: Conversation
  aiReplyTrigger: number
}>()

const emit = defineEmits<{
  (e: "ai-response-done"): void
  (e: "scroll-to", scrollIndex: number): void
}>()

const markdowntToHtml = (content?: string) =>
  markdownizer.markdownToHtml(content)

const currentAiReply = ref<string | undefined>()

const lastErrorMessage = ref<string | undefined>()

const aiStatus = ref<string | undefined>()

const toolCallResolver = ref<{
  resolve: (result: ToolCallResult) => void
  reject: (error: Error) => void
} | null>(null)

const currentSuggestion = ref<Suggestion | undefined>()

const scrollIndex = computed(
  () =>
    (currentAiReply.value ? currentAiReply.value.length : 0) +
    (lastErrorMessage.value ? 1 : 0) +
    (aiStatus.value ? 1 : 0) +
    (currentSuggestion.value ? 1 : 0)
)

watch(
  () => scrollIndex.value,
  (scrollIndex) => {
    emit("scroll-to", scrollIndex)
  }
)

const createAiActionContext = (): AiActionContext => ({
  set(text: string) {
    currentAiReply.value = text
  },
  append(text: string) {
    currentAiReply.value = currentAiReply.value
      ? currentAiReply.value + text
      : text
  },
  async reset() {
    emit("ai-response-done")
    currentAiReply.value = undefined
  },
  async handleSuggestion(suggestion: Suggestion) {
    currentSuggestion.value = suggestion
    return new Promise<ToolCallResult>((resolve, reject) => {
      toolCallResolver.value = { resolve, reject }
    })
  },
})

const clearToolCallState = () => {
  toolCallResolver.value = null
  currentSuggestion.value = undefined
}

const handleToolCallResolved = (result: ToolCallResult) => {
  toolCallResolver.value?.resolve(result)
  clearToolCallState()
}

const handleToolCallRejected = (error: Error) => {
  toolCallResolver.value?.reject(error)
  clearToolCallState()
}

const currentNote = computed(
  () =>
    conversation.subject?.note || conversation.subject?.answeredQuestion?.note
)

const getAiReply = async () => {
  const states = createAiReplyStates(createAiActionContext())

  aiStatus.value = "Starting AI reply..."
  new AiReplyEventSource(conversation.id)
    .onMessage(async (event, data) => {
      const state = states[event]
      if (state) {
        aiStatus.value = state.status
        await state.handleEvent(data)
      } else if (event === "error") {
        // Handle error event from SSE stream
        aiStatus.value = undefined
        lastErrorMessage.value = data || "Bad Request"
      } else {
        aiStatus.value = event
      }
    })
    .onError((e) => {
      aiStatus.value = undefined
      const error = e as Error
      if (
        error.message.indexOf("400") !== -1 ||
        error.message.indexOf("401") !== -1
      ) {
        lastErrorMessage.value = "Bad Request"
      }
    })
    .start()
}

watch(
  () => aiReplyTrigger,
  async () => {
    await getAiReply()
  }
)

defineExpose({
  getAiReply,
})
</script>

<style scoped>
.message-avatar {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-bar {
  background-color: #f8f9fa;
  padding: 0.5rem;
  border-radius: 0.25rem;
}

.user-message {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: inherit;
}

.completion-text {
  font-style: italic;
  color: #666;
}

.title-suggestion {
  font-style: italic;
  color: #666;
  font-weight: bold;
}

.unknown-request {
  font-family: monospace;
  background-color: #f8f9fa;
  padding: 0.5rem;
  border-radius: 0.25rem;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.unknown-request pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
}
</style>
