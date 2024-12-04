<template>
  <div v-if="currentAiReply" class="d-flex mb-3">
    <div class="message-avatar me-2" title="AI Assistant">
      <SvgRobot />
    </div>
    <div class="card py-2 px-3 bg-light ai-chat"
    v-html="markdowntToHtml(currentAiReply)"
    />
  </div>

  <div v-if="completionSuggestion" class="d-flex mb-3">
    <div class="message-avatar me-2" title="AI Assistant">
      <SvgRobot />
    </div>
    <AcceptRejectButtons
      :disabled="isProcessingToolCall"
      @accept="handleAcceptCompletion"
      @cancel="handleCancellation"
      @skip="handleSkip"
    >
      <template #title>
        Suggested completion:
      </template>
      <template #content>
        <div
          class="completion-text"
          v-html="markdowntToHtml(formattedCompletionSuggestion)"
        />
      </template>
    </AcceptRejectButtons>
  </div>

  <div v-if="lastErrorMessage" class="last-error-message text-danger mb-3">
    {{ lastErrorMessage }}
  </div>

  <div v-if="aiStatus" class="d-flex align-items-center status-bar mb-3">
    <div class="spinner-border spinner-border-sm me-2" role="status">
      <span class="visually-hidden">Loading...</span>
    </div>
    <small class="text-secondary">{{ aiStatus }}</small>
  </div>

  <div v-if="topicTitleSuggestion" class="d-flex mb-3">
    <div class="message-avatar me-2" title="AI Assistant">
      <SvgRobot />
    </div>
    <AcceptRejectButtons
      :disabled="isProcessingToolCall"
      @accept="handleAcceptTitle"
      @cancel="handleCancellation"
      @skip="handleSkip"
    >
      <template #title>
        Suggested title:
      </template>
      <template #content>
        <div class="title-suggestion">{{ topicTitleSuggestion }}</div>
      </template>
    </AcceptRejectButtons>
  </div>

  <div v-if="unknownRequestSuggestion" class="d-flex mb-3">
    <div class="message-avatar me-2" title="AI Assistant">
      <SvgRobot />
    </div>
    <AcceptRejectButtons
      :disabled="isProcessingToolCall"
      @cancel="handleCancellation"
      @skip="handleSkip"
      :hideAccept="true"
    >
      <template #title>
        Unknown tool call: {{ unknownRequestSuggestion.functionName }}
      </template>
      <template #content>
        <div class="unknown-request">
          <pre>{{ unknownRequestSuggestion.rawJson }}</pre>
        </div>
      </template>
    </AcceptRejectButtons>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type {
  Conversation,
  Note,
  NoteDetailsCompletion,
  ToolCallResult,
} from "@/generated/backend"
import SvgRobot from "@/components/svgs/SvgRobot.vue"
import type { StorageAccessor } from "@/store/createNoteStorage"
import markdownizer from "../form/markdownizer"
import {
  createAiReplyStates,
  type AiActionContext,
} from "@/models/aiReplyState"
import AcceptRejectButtons from "@/components/commons/AcceptRejectButtons.vue"

const { conversation, storageAccessor, aiReplyTrigger } = defineProps<{
  conversation: Conversation
  storageAccessor: StorageAccessor
  aiReplyTrigger: number
}>()

const emit = defineEmits<{
  (e: "ai-response-done"): void
  (e: "scroll-to", scrollIndex: number): void
}>()

const { managedApi } = useLoadingApi()

const markdowntToHtml = (content?: string) =>
  markdownizer.markdownToHtml(content)

const currentAiReply = ref<string | undefined>()

const lastErrorMessage = ref<string | undefined>()

const aiStatus = ref<string | undefined>()

const completionSuggestion = ref<NoteDetailsCompletion | undefined>()

const topicTitleSuggestion = ref<string | undefined>()

const unknownRequestSuggestion = ref<
  | {
      rawJson: string
      functionName: string
    }
  | undefined
>()

const isProcessingToolCall = ref(false)

const pendingToolCall = ref<
  | {
      threadId: string
      runId: string
      toolCallId: string
    }
  | undefined
>()

const toolCallResolver = ref<{
  resolve: (result: ToolCallResult) => void
  reject: (error: Error) => void
} | null>(null)

const scrollIndex = computed(
  () =>
    (currentAiReply.value ? currentAiReply.value.length : 0) +
    (completionSuggestion.value ? 1 : 0) +
    (lastErrorMessage.value ? 1 : 0) +
    (aiStatus.value ? 1 : 0) +
    (topicTitleSuggestion.value ? 1 : 0) +
    (unknownRequestSuggestion.value ? 1 : 0)
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
  async appendNoteDetails(completion, threadId, runId, toolCallId) {
    completionSuggestion.value = completion
    pendingToolCall.value = { threadId, runId, toolCallId }
    return createToolCallPromise()
  },
  async setTopicTitle(title, threadId, runId, toolCallId) {
    topicTitleSuggestion.value = title
    pendingToolCall.value = { threadId, runId, toolCallId }
    return createToolCallPromise()
  },
  async unknownRequest(rawJson, functionName, threadId, runId, toolCallId) {
    unknownRequestSuggestion.value = { rawJson, functionName }
    pendingToolCall.value = { threadId, runId, toolCallId }
    return createToolCallPromise()
  },
})

const createToolCallPromise = () => {
  return new Promise<ToolCallResult>((resolve, reject) => {
    toolCallResolver.value = { resolve, reject }
  })
}

const clearToolCallState = () => {
  completionSuggestion.value = undefined
  topicTitleSuggestion.value = undefined
  unknownRequestSuggestion.value = undefined
  pendingToolCall.value = undefined
  toolCallResolver.value = null
}

const handleToolCallAccept = async (action: (note: Note) => Promise<void>) => {
  if (!pendingToolCall.value || isProcessingToolCall.value) return

  try {
    isProcessingToolCall.value = true
    const note =
      conversation.subject?.note || conversation.subject?.answeredQuestion?.note
    if (!note) {
      console.error("No note found in conversation")
      return
    }

    await action(note)
    toolCallResolver.value?.resolve({ status: "accepted" })
    clearToolCallState()
  } finally {
    isProcessingToolCall.value = false
  }
  emit("ai-response-done")
}

const handleCancellation = async () => {
  if (!pendingToolCall.value || isProcessingToolCall.value) return

  try {
    isProcessingToolCall.value = true
    toolCallResolver.value?.reject(new Error("Tool call was rejected"))
    clearToolCallState()
  } finally {
    isProcessingToolCall.value = false
  }
}

const handleAcceptCompletion = () => {
  if (!completionSuggestion.value) return
  return handleToolCallAccept(async (note) => {
    await storageAccessor
      .storedApi()
      .completeDetails(note.id, completionSuggestion.value!)
  })
}

const handleAcceptTitle = () => {
  if (!topicTitleSuggestion.value) return
  return handleToolCallAccept(async (note) => {
    await storageAccessor
      .storedApi()
      .updateTextField(note.id, "edit topic", topicTitleSuggestion.value!)
  })
}

const handleSkip = async () => {
  if (!pendingToolCall.value || isProcessingToolCall.value) return

  try {
    isProcessingToolCall.value = true
    toolCallResolver.value?.resolve({ status: "skipped" })
    clearToolCallState()
  } finally {
    isProcessingToolCall.value = false
  }
}

const getAiReply = async () => {
  const states = createAiReplyStates(
    createAiActionContext(),
    managedApi.restAiController
  )

  aiStatus.value = "Starting AI reply..."
  await managedApi.eventSource
    .onMessage(async (event, data) => {
      const state = states[event]
      if (state) {
        aiStatus.value = state.status
        await state.handleEvent(data)
      } else {
        aiStatus.value = event
      }
    })
    .onError((e) => {
      aiStatus.value = undefined
      const error = e as Error
      if (error.message.indexOf("400") !== -1) {
        lastErrorMessage.value = "Bad Request"
      }
    })
    .restConversationMessageController.getAiReply(conversation.id)
}

watch(
  () => aiReplyTrigger,
  async () => {
    await getAiReply()
  }
)

const formattedCompletionSuggestion = computed(() => {
  if (!completionSuggestion.value) return ""
  const currentDetails = conversation.subject?.note?.details?.trim() || ""
  const { completion, deleteFromEnd } = completionSuggestion.value

  if (!currentDetails) return completion

  if (!deleteFromEnd) return `...${completion}`

  // Get the text to be deleted (last N characters)
  const textToDelete = currentDetails.slice(-deleteFromEnd) || currentDetails

  // Replace spaces and newlines with placeholder characters
  const strikeThroughText = textToDelete.replace(/ /g, "·").replace(/\n/g, "↵")

  // Format with markdown strikethrough using the placeholder
  return `~~${strikeThroughText}~~${completion}`
})

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
