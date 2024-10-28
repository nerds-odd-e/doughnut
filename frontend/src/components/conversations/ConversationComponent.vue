<template>
  <div class="px-3 py-3 conversations" v-if="currentConversationMessages">
    <div
      v-for="conversationMessage in currentConversationMessages"
      :key="conversationMessage.id"
      class="d-flex mb-3"
      :class="{ 'justify-content-end': isCurrentUser(conversationMessage.sender?.id || 0) }"
    >
      <div
        class="card py-2 px-3"
        :class="[
          isCurrentUser(conversationMessage.sender?.id || 0) ? 'text-bg-dark' : 'bg-light',
          conversationMessage.sender?.id === undefined ? 'ai-chat' : '',
        ]"
      >
        <template v-if="conversationMessage.sender?.id === undefined">
          <SvgRobot />
        </template>
        {{ formatMessage(conversationMessage.message) }}
      </div>
    </div>

    <div class="chat-controls">
      <form class="row chat-input-container" @submit.prevent="handleSendMessage()">
        <div class="col-md-10">
          <textarea class="w-100" name="Description" v-model="message" />
        </div>
        <div class="col-md-1">
          <input
            type="submit"
            value="Send"
            id="chat-button"
            class="btn float-btn btn-secondary"
          />
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type {
  User,
  ConversationMessage,
  Conversation,
} from "@/generated/backend"
import SvgRobot from "@/components/svgs/SvgRobot.vue"

const props = defineProps<{
  conversation: Conversation
  user: User
}>()

const emit = defineEmits<{
  (e: "conversation-fetched", conversationId: number): void
}>()

const { managedApi } = useLoadingApi()

const currentConversationMessages = ref<ConversationMessage[] | undefined>(
  undefined
)
const message = ref("")

const formatMessage = (message: string) => {
  return message.replace(/^"|"$/g, "").trim()
}

const isCurrentUser = (id: number): boolean => {
  return id === props.user?.id
}

const fetchConversationMessages = async () => {
  currentConversationMessages.value =
    await managedApi.restConversationMessageController.getConversationMessages(
      props.conversation.id
    )
  emit("conversation-fetched", props.conversation.id)
}

const handleSendMessage = async () => {
  await managedApi.restConversationMessageController.replyToConversation(
    props.conversation.id,
    message.value
  )
  message.value = ""
  await fetchConversationMessages()
}

onMounted(() => {
  fetchConversationMessages()
})

watch(() => props.conversation, fetchConversationMessages)
</script>

<style scoped>
.conversations {
  margin-bottom: 100px;
}

.ai-chat {
  color: red;
}

.chat-controls {
  position: fixed;
  width: 75%;
  bottom: 0;
  right: 0;
  background-color: white;
  box-shadow: 0 -2px 4px rgba(0, 0, 0, 0.1);
  padding: 10px;
}
</style>
