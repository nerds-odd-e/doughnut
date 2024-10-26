<template>
  <ContainerFluidPage
    v-bind="{
      contentExists: true,
      title: 'Message Center',
    }"
  >
    <h2 v-if="!conversations?.length" class="info-heading">There is no feedback currently.</h2>

    <template v-if="conversations?.length">
      <div class="message-center-container">
        <div class="sidebar">
          <ul class="list-group">
            <li
              v-for="conversation in conversations"
              :key="conversation.id"
              class="list-group-item list-group-item-action"
              @click="currentConversationId = conversation.id"
            >
              <div>{{ conversationTopic(conversation) }}</div>
              <div>{{ conversationPartner(conversation) }}</div>
            </li>
          </ul>
        </div>

        <div class="main-content">
          <ConversationComponent
            v-if="currentConversationId && user"
            :conversation-id="currentConversationId"
            :user="user"
            @conversation-fetched="handleConversationFetched"
          />
          <h2 class="info-heading" v-else>No conversation selected</h2>
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
.message-center-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 150px); /* Adjust this value based on your layout */
  min-height: 400px; /* Ensure a minimum height on smaller screens */
}

.sidebar {
  flex: 0 0 auto;
  overflow-y: auto;
  max-height: 300px; /* Limit height on mobile */
}

.main-content {
  flex: 1 1 auto;
  overflow-y: auto;
  min-height: 200px; /* Ensure minimum height for content */
}

@media (min-width: 768px) {
  .message-center-container {
    flex-direction: row;
  }

  .sidebar {
    flex: 0 0 25%;
    max-height: none; /* Remove max-height limit on larger screens */
  }

  .main-content {
    flex: 0 0 75%;
  }
}

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

.info-heading {
  padding: 10px 20px;
  display: inline-block;
  color: #808080; /* Grayed out color */

}
</style>

ai-chat
