<template>
  <ContainerFluidPage
    v-bind="{
      contentExists: true,
      title: 'Message Center',
    }"
  >
    <h2 v-if="!conversations?.length">There is no feedback currently.</h2>

    <template v-if="conversations?.length">
      <div class="row g-0 h-100">
        <div class="col-md-3 bg-light sidebar">
          <ul class="list-group">
            <li v-for="conversation in conversations" :key="conversation.id" class="list-group-item list-group-item-action" @click="fetchThreadsForConversation(conversation.id || 0)">
              <div>{{ conversationTopic(conversation) }}</div>
              <div>{{ conversationPartner(conversation) }}</div>
            </li>
          </ul>
        </div>

        <div class="col-md-9 main-content">
          <div class="px-3 py-3 conversations" v-if="currentConversationDetails">
            <div v-for="thread in currentConversationDetails" :key="thread.id" class="d-flex mb-3" :class="{ 'justify-content-end': isCurrentUser(thread.sender?.id || 0) }">
              <div class="card py-2 px-3" :class="[isCurrentUser(thread.sender?.id || 0) ? 'text-bg-dark': 'bg-light', thread.sender?.id === undefined ? 'ai-chat' : '']">
                <template v-if="thread.sender?.id === undefined">
                  <SvgRobot />
                </template>
                {{ formatMessage(thread.message) }}
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
          <h2 v-else>No conversation selected</h2>
        </div>
      </div>
    </template>
  </ContainerFluidPage>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { onMounted, ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import ContainerFluidPage from "@/pages/commons/ContainerFluidPage.vue"
import type {
  Conversation,
  User,
  ConversationMessage,
} from "@/generated/backend"
import { messageCenterConversations } from "@/store/messageStore"

const { managedApi } = useLoadingApi()

const { user } = defineProps({
  user: { type: Object as PropType<User> },
})

const conversations = ref<Conversation[] | undefined>(undefined)
const currentConversationId = ref(0)
const currentConversationDetails = ref<ConversationMessage[] | undefined>(
  undefined
)
const message = ref("")

const formatMessage = (message: string) => {
  return message.replace(/^"|"$/g, "").trim()
}

const isCurrentUser = (id: number): boolean => {
  return id === user?.id
}

const fetchData = async () => {
  conversations.value =
    await managedApi.restConversationMessageController.getConversationsOfCurrentUser()
}

const handleSendMessage = async () => {
  await managedApi.restConversationMessageController.replyToConversation(
    currentConversationId.value,
    message.value
  )
  message.value = ""
  await fetchThreadsForConversation(currentConversationId.value)
}

const fetchThreadsForConversation = async (conversationId: number) => {
  currentConversationDetails.value =
    await managedApi.restConversationMessageController.getConversationDetails(
      conversationId
    )
  currentConversationId.value = conversationId
  messageCenterConversations.unreadConversations =
    await managedApi.restConversationMessageController.markConversationAsRead(
      conversationId
    )
}

onMounted(() => {
  fetchData()
})

const conversationTopic = (conversation: Conversation) => {
  return (
    conversation.assessmentQuestionInstance?.bareQuestion
      .multipleChoicesQuestion.stem ||
    conversation.note?.noteTopic?.topicConstructor
  )
}

const conversationPartner = (conversation: Conversation) => {
  if (conversation.subjectOwnership?.circle?.name) {
    return conversation.subjectOwnership.circle.name
  }
  if (conversation.conversationInitiator?.name !== user?.name) {
    return conversation.conversationInitiator?.name
  }
  return conversation.subjectOwnership?.ownerName
}
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

ai-chat
