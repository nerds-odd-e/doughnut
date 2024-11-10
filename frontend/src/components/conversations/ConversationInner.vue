<template>
  <ConversationTemplate
    @send-message="handleSendMessage"
    @send-message-and-invite-ai="handleSendMessageAndInviteAI"
    @close-dialog="$emit('close-dialog')"
    @conversation-changed="$emit('conversation-changed', $event)"
    @new-conversation="$emit('new-conversation')"
    @toggle-maximize="$emit('toggle-maximize')"
    :is-maximized="isMaximized"
    :conversations="conversations"
    :selectedConversation="conversation"
    :allow-new-conversation="allowNewConversation"
  >
    <template #messages v-if="currentConversationMessages !== undefined">
      <div
        v-for="conversationMessage in currentConversationMessages"
        :key="conversationMessage.id"
        class="d-flex mb-3"
        :class="{ 'justify-content-end': isCurrentUser(conversationMessage.sender?.id || 0) }"
      >
        <div
          v-if="!isCurrentUser(conversationMessage.sender?.id || 0)"
          class="message-avatar me-2"
          :title="conversationMessage.sender?.name || 'AI Assistant'"
        >
          <template v-if="conversationMessage.sender?.id === undefined">
            <SvgRobot />
          </template>
          <template v-else>
            <SvgMissingAvatar />
          </template>
        </div>

        <div
          class="card py-2 px-3"
          :class="[
            isCurrentUser(conversationMessage.sender?.id || 0) ? 'text-bg-dark' : 'bg-light',
            conversationMessage.sender?.id === undefined ? 'ai-chat' : '',
          ]"
        >
          <pre v-if="isCurrentUser(conversationMessage.sender?.id || 0)" class="user-message">{{ formatMessage(conversationMessage.message) }}</pre>
          <div v-else v-html="markdowntToHtml(formatMessage(conversationMessage.message))" />
        </div>
      </div>

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
        <div class="card py-2 px-3 bg-light ai-chat">
          <div>Suggested completion:</div>
          <div class="completion-text mb-2" v-html="markdowntToHtml(formattedCompletionSuggestion)" />
          <div class="d-flex gap-2">
            <button
              class="btn btn-primary btn-sm"
              @click="handleAcceptCompletion"
              :disabled="isProcessingToolCall"
            >
              Accept
            </button>
            <button
              class="btn btn-secondary btn-sm"
              @click="handleRejectCompletion"
              :disabled="isProcessingToolCall"
            >
              Reject
            </button>
          </div>
        </div>
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
        <div class="card py-2 px-3 bg-light ai-chat">
          <div>Suggested title:</div>
          <div class="title-suggestion mb-2">{{ topicTitleSuggestion }}</div>
          <div class="d-flex gap-2">
            <button
              class="btn btn-primary btn-sm"
              @click="handleAcceptTitle"
              :disabled="isProcessingToolCall"
            >
              Accept
            </button>
            <button
              class="btn btn-secondary btn-sm"
              @click="handleRejectTitle"
              :disabled="isProcessingToolCall"
            >
              Reject
            </button>
          </div>
        </div>
      </div>

      <ScrollTo :scrollTrigger="currentConversationMessages.length + (currentAiReply ? currentAiReply.length : 0) + (completionSuggestion ? 1 : 0) + (lastErrorMessage ? 1 : 0) + (aiStatus ? 1 : 0) + (topicTitleSuggestion ? 1 : 0)" />
    </template>
  </ConversationTemplate>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type {
  User,
  ConversationMessage,
  Conversation,
} from "@/generated/backend"
import SvgRobot from "@/components/svgs/SvgRobot.vue"
import ScrollTo from "@/components/commons/ScrollTo.vue"
import type { StorageAccessor } from "@/store/createNoteStorage"
import SvgMissingAvatar from "@/components/svgs/SvgMissingAvatar.vue"
import ConversationTemplate from "./ConversationTemplate.vue"
import markdownizer from "../form/markdownizer"
import { createAiReplyStates, AiActionContext } from "@/models/aiReplyState"

const { conversation, user, initialAiReply, storageAccessor, isMaximized } =
  defineProps<{
    conversation: Conversation
    conversations?: Conversation[]
    user: User
    storageAccessor: StorageAccessor
    allowNewConversation?: boolean
    initialAiReply?: boolean
    isMaximized?: boolean
  }>()

const emit = defineEmits<{
  (e: "conversation-fetched", conversationId: number): void
  (e: "close-dialog"): void
  (e: "conversation-changed", conversationId: number): void
  (e: "new-conversation"): void
  (e: "toggle-maximize"): void
}>()

const { managedApi } = useLoadingApi()

const currentConversationMessages = ref<ConversationMessage[] | undefined>(
  undefined
)

const markdowntToHtml = (content?: string) =>
  markdownizer.markdownToHtml(content)

const currentAiReply = ref<string | undefined>()

const lastErrorMessage = ref<string | undefined>()

const aiStatus = ref<string | undefined>()

const completionSuggestion = ref<string | undefined>()
let pendingCompletionData:
  | { threadId: string; runId: string; toolCallId: string }
  | undefined

const topicTitleSuggestion = ref<string | undefined>()
let pendingTitleData:
  | { threadId: string; runId: string; toolCallId: string }
  | undefined

const isProcessingToolCall = ref(false)

const formatMessage = (message: string) => {
  return message.replace(/^"|"$/g, "").trim()
}

const isCurrentUser = (id: number): boolean => {
  return id === user?.id
}

const fetchConversationMessages = async () => {
  if (!conversation.id) return

  currentConversationMessages.value =
    await managedApi.restConversationMessageController.getConversationMessages(
      conversation.id
    )
  emit("conversation-fetched", conversation.id)
}

const handleSendMessage = async (
  message: string,
  inviteAI: boolean = false
) => {
  await managedApi.restConversationMessageController.replyToConversation(
    conversation.id,
    message
  )
  await fetchConversationMessages()

  if (inviteAI) {
    await getAiReply()
  }
}

const getAiReply = async () => {
  const aiActionContext: AiActionContext = {
    set(text: string) {
      currentAiReply.value = text
    },
    append(text: string) {
      if (!currentAiReply.value) {
        currentAiReply.value = text
      } else {
        currentAiReply.value += text
      }
    },
    async reset() {
      await fetchConversationMessages()
      currentAiReply.value = undefined
    },
    async appendNoteDetails(
      completion: string,
      threadId: string,
      runId: string,
      toolCallId: string
    ) {
      completionSuggestion.value = completion
      pendingCompletionData = { threadId, runId, toolCallId }
    },
    async setTopicTitle(
      title: string,
      threadId: string,
      runId: string,
      toolCallId: string
    ) {
      topicTitleSuggestion.value = title
      pendingTitleData = { threadId, runId, toolCallId }
    },
  }

  const states = createAiReplyStates(aiActionContext)

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

const handleAcceptCompletion = async () => {
  if (
    !completionSuggestion.value ||
    !pendingCompletionData ||
    isProcessingToolCall.value
  )
    return

  try {
    isProcessingToolCall.value = true
    const { threadId, runId, toolCallId } = pendingCompletionData
    const note = conversation.subject?.note
    if (!note) {
      console.error("No note found in conversation")
      return
    }

    await storageAccessor
      .storedApi()
      .appendDetails(note.id, completionSuggestion.value)
    await managedApi.restAiController.submitToolCallResult(
      threadId,
      runId,
      toolCallId,
      { status: "accepted" }
    )

    completionSuggestion.value = undefined
    pendingCompletionData = undefined
  } finally {
    isProcessingToolCall.value = false
  }
  await fetchConversationMessages()
}

const handleRejectCompletion = async () => {
  if (!pendingCompletionData || isProcessingToolCall.value) return

  try {
    isProcessingToolCall.value = true
    const { threadId, runId } = pendingCompletionData
    await managedApi.restAiController.cancelRun(threadId, runId)

    completionSuggestion.value = undefined
    pendingCompletionData = undefined
  } finally {
    isProcessingToolCall.value = false
  }
}

const formattedCompletionSuggestion = computed(() => {
  if (!completionSuggestion.value) return ""
  const currentDetails = conversation.subject?.note?.details?.trim() || ""
  return currentDetails
    ? `...${completionSuggestion.value}`
    : completionSuggestion.value
})

const handleAcceptTitle = async () => {
  if (
    !topicTitleSuggestion.value ||
    !pendingTitleData ||
    isProcessingToolCall.value
  )
    return

  try {
    isProcessingToolCall.value = true
    const { threadId, runId, toolCallId } = pendingTitleData
    const note = conversation.subject?.note
    if (!note) {
      console.error("No note found in conversation")
      return
    }

    await storageAccessor
      .storedApi()
      .updateTextField(note.id, "edit topic", topicTitleSuggestion.value)
    await managedApi.restAiController.submitToolCallResult(
      threadId,
      runId,
      toolCallId,
      { status: "accepted" }
    )

    topicTitleSuggestion.value = undefined
    pendingTitleData = undefined
  } finally {
    isProcessingToolCall.value = false
  }
  await fetchConversationMessages()
}

const handleRejectTitle = async () => {
  if (!pendingTitleData || isProcessingToolCall.value) return

  try {
    isProcessingToolCall.value = true
    const { threadId, runId } = pendingTitleData
    await managedApi.restAiController.cancelRun(threadId, runId)

    topicTitleSuggestion.value = undefined
    pendingTitleData = undefined
  } finally {
    isProcessingToolCall.value = false
  }
}

onMounted(async () => {
  await fetchConversationMessages()
  if (initialAiReply) {
    await getAiReply()
  }
})

watch(() => conversation, fetchConversationMessages)

const handleSendMessageAndInviteAI = async (message: string) => {
  await handleSendMessage(message, true)
}
</script>

<style scoped>
.ai-chat {
  background-color: #f8f9fa;
  border-left: 3px solid #0d6efd;
}

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
</style>
