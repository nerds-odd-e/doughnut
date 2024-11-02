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
    <template #messages v-if="currentConversationMessages">
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
          {{ formatMessage(conversationMessage.message) }}
        </div>
      </div>

      <div v-if="currentAiReply" class="d-flex mb-3">
        <div class="message-avatar me-2" title="AI Assistant">
          <SvgRobot />
        </div>
        <div class="card py-2 px-3 bg-light ai-chat">
          {{ formatMessage(currentAiReply) }}
        </div>
      </div>

      <ScrollTo :scrollTrigger="currentConversationMessages.length + (currentAiReply ? 1 : 0)" />
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
} from "@/generated/backend"
import SvgRobot from "@/components/svgs/SvgRobot.vue"
import ScrollTo from "@/components/commons/ScrollTo.vue"
import type { StorageAccessor } from "@/store/createNoteStorage"
import SvgMissingAvatar from "@/components/svgs/SvgMissingAvatar.vue"
import ConversationTemplate from "./ConversationTemplate.vue"

const props = defineProps<{
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

const currentAiReply = ref<string | undefined>()

const formatMessage = (message: string) => {
  return message.replace(/^"|"$/g, "").trim()
}

const isCurrentUser = (id: number): boolean => {
  return id === props.user?.id
}

const fetchConversationMessages = async () => {
  if (!props.conversation.id) return

  currentConversationMessages.value =
    await managedApi.restConversationMessageController.getConversationMessages(
      props.conversation.id
    )
  emit("conversation-fetched", props.conversation.id)
}

const handleSendMessage = async (
  message: string,
  inviteAI: boolean = false
) => {
  await managedApi.restConversationMessageController.replyToConversation(
    props.conversation.id,
    message
  )
  await fetchConversationMessages()

  if (inviteAI) {
    await getAiReply()
  }
}

const getAiReply = async () => {
  await managedApi.eventSource
    .onMessage((event, data) => {
      if (event === "thread.message.created") {
        const response = JSON.parse(data) as Message
        response.content = [{ text: { value: "" } }]
        currentAiReply.value = response.content?.[0]?.text?.value
      }
      if (event === "thread.message.delta") {
        const response = JSON.parse(data) as MessageDelta
        const delta = response.delta?.content?.[0]?.text?.value
        currentAiReply.value = currentAiReply.value! + delta
      }
    })
    .onError((error) => {
      // eslint-disable-next-line no-console
      console.error(error)
    })
    .restConversationMessageController.getAiReply(props.conversation.id)
}

onMounted(async () => {
  await fetchConversationMessages()
  if (props.initialAiReply) {
    await getAiReply()
  }
})

watch(() => props.conversation, fetchConversationMessages)

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
</style>
