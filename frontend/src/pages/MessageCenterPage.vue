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
            <li v-for="conversation in conversations" :key="conversation.id" class="list-group-item list-group-item-action" @click="fetchConversationMessages(conversation.id || 0)">
              <div>{{ conversationTopic(conversation) }}</div>
              <div>{{ conversationPartner(conversation) }}</div>
            </li>
          </ul>
        </div>

        <div class="col-md-9 main-content">
          <ConversationComponent
            v-if="currentConversationId && user"
            :conversation-id="currentConversationId"
            :user="user"
            @conversation-fetched="handleConversationFetched"
          />
          <h2 v-else>No conversation selected</h2>
        </div>
      </div>
    </template>
  </ContainerFluidPage>
</template>

<script setup lang="ts">
import { onMounted, ref, type PropType } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import ContainerFluidPage from "@/pages/commons/ContainerFluidPage.vue"
import ConversationComponent from "@/components/conversations/ConversationComponent.vue"
import type { Conversation, User } from "@/generated/backend"
import { messageCenterConversations } from "@/store/messageStore"

const { managedApi } = useLoadingApi()

const { user } = defineProps({
  user: { type: Object as PropType<User> },
})

const conversations = ref<Conversation[] | undefined>(undefined)
const currentConversationId = ref<number | null>(null)

const fetchData = async () => {
  conversations.value =
    await managedApi.restConversationMessageController.getConversationsOfCurrentUser()
}

const fetchConversationMessages = async (conversationId: number) => {
  currentConversationId.value = conversationId
}

const handleConversationFetched = async (conversationId: number) => {
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
  if (conversation.conversationInitiator?.name !== user?.name) {
    return conversation.conversationInitiator?.name
  }
  if (conversation.subjectOwnership?.circle?.name) {
    return conversation.subjectOwnership.circle.name
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
