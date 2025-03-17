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
    :default-messages="showDefaultMessages ? defaultQuestions : undefined"
  >
    <template #messages v-if="currentConversationMessages !== undefined">
      <div
        v-for="conversationMessage in currentConversationMessages"
        :key="conversationMessage.id"
        class="daisy-flex daisy-mb-3"
        :class="{ 'daisy-justify-end': isCurrentUser(conversationMessage.sender?.id || 0) }"
      >
        <div
          v-if="!isCurrentUser(conversationMessage.sender?.id || 0)"
          class="message-avatar daisy-me-2"
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
          class="daisy-card daisy-py-2 daisy-px-3"
          :class="[
            isCurrentUser(conversationMessage.sender?.id || 0) ? 'daisy-text-bg-dark' : 'daisy-bg-light',
            conversationMessage.sender?.id === undefined ? 'ai-chat' : '',
          ]"
        >
          <pre v-if="isCurrentUser(conversationMessage.sender?.id || 0)" class="user-message">{{ formatMessage(conversationMessage.message) }}</pre>
          <div v-else v-html="markdowntToHtml(formatMessage(conversationMessage.message))" />
        </div>
      </div>

      <AiResponse
        :conversation="conversation"
        :storageAccessor="storageAccessor"
        :aiReplyTrigger="aiReplyTrigger"
        @ai-response-done="onAiResponseDone"
        @scroll-to="scrollIndex = $event"
      />

      <ScrollTo :scrollTrigger="currentConversationMessages.length + scrollIndex" />
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

const aiReplyTrigger = ref(0)

const scrollIndex = ref(0)

const onAiResponseDone = () => {
  fetchConversationMessages()
}

const markdowntToHtml = (content?: string) =>
  markdownizer.markdownToHtml(content)

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
    aiReplyTrigger.value++
  }
}

onMounted(async () => {
  await fetchConversationMessages()
  if (initialAiReply) {
    aiReplyTrigger.value++
  }
})

watch(() => conversation, fetchConversationMessages)

const handleSendMessageAndInviteAI = (message: string) =>
  handleSendMessage(message, true)

const defaultQuestions = [
  "Why is my answer wrong?",
  "I think the question is wrong. Please check.",
  "Please explain the question and answer to me.",
  "Why are other choices incorrect?",
]

const showDefaultMessages = computed(() => {
  return (
    conversation.subject?.answeredQuestion &&
    (!currentConversationMessages.value ||
      currentConversationMessages.value.length === 0)
  )
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

.user-message {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: inherit;
}

.unknown-request {
  font-family: monospace;
  background-color: #f0f0f0; /* Light gray background as a fallback */
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
