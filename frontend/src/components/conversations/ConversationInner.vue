<template>
  <ConversationTemplate
    @send-message="handleSendMessage"
    @send-message-and-invite-ai="handleSendMessageAndInviteAI"
    @close-dialog="$emit('close-dialog')"
    @conversation-changed="$emit('conversation-changed', $event)"
    @new-conversation="$emit('new-conversation')"
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

      <div v-if="lastErrorMessage" class="last-error-message text-danger mb-3">
        {{ lastErrorMessage }}
      </div>

      <div v-if="aiStatus" class="d-flex align-items-center status-bar mb-3">
        <div class="spinner-border spinner-border-sm me-2" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
        <small class="text-secondary">{{ aiStatus }}</small>
      </div>

      <ScrollTo :scrollTrigger="currentConversationMessages.length + (currentAiReply ? currentAiReply.length : 0) + (lastErrorMessage ? 1 : 0) + (aiStatus ? 1 : 0)" />
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
  Message,
  MessageDelta,
  Run,
  NoteDetailsCompletion,
} from "@/generated/backend"
import SvgRobot from "@/components/svgs/SvgRobot.vue"
import ScrollTo from "@/components/commons/ScrollTo.vue"
import type { StorageAccessor } from "@/store/createNoteStorage"
import SvgMissingAvatar from "@/components/svgs/SvgMissingAvatar.vue"
import ConversationTemplate from "./ConversationTemplate.vue"
import markdownizer from "../form/markdownizer"

const { conversation, user, initialAiReply, storageAccessor } = defineProps<{
  conversation: Conversation
  conversations?: Conversation[]
  user: User
  storageAccessor: StorageAccessor
  allowNewConversation?: boolean
  initialAiReply?: boolean
}>()

const emit = defineEmits<{
  (e: "conversation-fetched", conversationId: number): void
  (e: "close-dialog"): void
  (e: "conversation-changed", conversationId: number): void
  (e: "new-conversation"): void
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
  aiStatus.value = "Starting AI reply..."
  await managedApi.eventSource
    .onMessage((event, data) => {
      if (event === "thread.message.created") {
        aiStatus.value = "Generating response..."
        const response = JSON.parse(data) as Message
        response.content = [{ text: { value: "" } }]
        currentAiReply.value = response.content?.[0]?.text?.value
      }
      if (event === "thread.message.delta") {
        aiStatus.value = "Writing response..."
        const response = JSON.parse(data) as MessageDelta
        const delta = response.delta?.content?.[0]?.text?.value
        currentAiReply.value = currentAiReply.value! + delta
      }
      if (event === "thread.run.requires_action") {
        aiStatus.value = "Processing actions..."
        const note = conversation.subject?.note
        if (!note) {
          console.error("No note found in conversation")
          return
        }
        const response = JSON.parse(data) as Run
        const contentToAppend = JSON.parse(
          response.required_action!.submit_tool_outputs!.tool_calls![0]!
            .function!.arguments as unknown as string
        ) as NoteDetailsCompletion
        storageAccessor
          .storedApi()
          .appendDetails(note.id, contentToAppend!.completion)
      }
      if (event === "done") {
        aiStatus.value = undefined
        fetchConversationMessages().then(() => {
          currentAiReply.value = undefined
        })
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

const handleSendMessageAndInviteAI = async (message: string) => {
  await handleSendMessage(message, true)
}
</script>

<style scoped>
.ai-chat {
  color: red;
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
</style>
