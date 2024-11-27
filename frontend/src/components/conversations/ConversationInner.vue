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

      <AiResponses
        :current-ai-reply="currentAiReply"
        :completion-suggestion="completionSuggestion"
        :ai-status="aiStatus"
        :topic-title-suggestion="topicTitleSuggestion"
        :unknown-request-suggestion="unknownRequestSuggestion"
        :last-error-message="lastErrorMessage"
        :is-processing-tool-call="isProcessingToolCall"
        :current-details="conversation.subject?.note?.details?.trim()"
        @accept-completion="handleAcceptCompletion"
        @accept-title="handleAcceptTitle"
        @cancel="handleCancellation"
        @skip="handleSkip"
        @scroll-index="scrollIndex = $event"
      />

      <ScrollTo :scrollTrigger="scrollIndex + currentConversationMessages.length + (currentAiReply ? currentAiReply.length : 0) + (completionSuggestion ? 1 : 0) + (lastErrorMessage ? 1 : 0) + (aiStatus ? 1 : 0) + (topicTitleSuggestion ? 1 : 0) + (unknownRequestSuggestion ? 1 : 0)"/>
    </template>
  </ConversationTemplate>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type {
  User,
  ConversationMessage,
  Conversation,
  Note,
  ToolCallResult,
} from "@/generated/backend"
import SvgRobot from "@/components/svgs/SvgRobot.vue"
import ScrollTo from "@/components/commons/ScrollTo.vue"
import type { StorageAccessor } from "@/store/createNoteStorage"
import SvgMissingAvatar from "@/components/svgs/SvgMissingAvatar.vue"
import ConversationTemplate from "./ConversationTemplate.vue"
import markdownizer from "../form/markdownizer"
import {
  createAiReplyStates,
  type AiActionContext,
} from "@/models/aiReplyState"
import AiResponses from "./AiResponses.vue"

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

const scrollIndex = ref(0)

const currentAiReply = ref<string | undefined>()

const lastErrorMessage = ref<string | undefined>()

const aiStatus = ref<string | undefined>()

const completionSuggestion = ref<string | undefined>()

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
    await fetchConversationMessages()
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
    const note = conversation.subject?.note
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
  await fetchConversationMessages()
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
      .appendDetails(note.id, completionSuggestion.value!)
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

onMounted(async () => {
  await fetchConversationMessages()
  if (initialAiReply) {
    await getAiReply()
  }
})

watch(() => conversation, fetchConversationMessages)

const handleSendMessageAndInviteAI = (message: string) =>
  handleSendMessage(message, true)
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
