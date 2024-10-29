<template>
  <ContainerPage
    v-bind="{
      fluid: true,
      contentExists: true,
      title: 'Message Center',
    }"
  >
    <h2 v-if="!conversations?.length" class="info-heading">
      There is no conversation currently.
    </h2>

    <template v-else>
      <div class="message-center-container">
        <div class="sidebar" :class="{ 'hide-on-mobile': !showSidebarOnMobile }">
          <ul class="list-group">
            <li
              v-for="conversation in conversations"
              :key="conversation.id"
              class="list-group-item list-group-item-action"
              :class="{ 'active': conversationId === conversation.id }"
              @click="selectConversation(conversation)"
            >
              <div>{{ conversationTopic(conversation) }}</div>
              <div>{{ conversationPartner(conversation) }}</div>
            </li>
          </ul>
        </div>

        <div class="main-content" :class="{ 'hide-on-mobile': !showMainContentOnMobile }">
          <div class="mobile-back-button" @click="backToList">
            <span>&larr; Back to conversations</span>
          </div>
          <ConversationComponent
            v-if="currentConversation && user"
            :conversation="currentConversation"
            :user="user"
            :storageAccessor="storageAccessor"
            @conversation-fetched="handleConversationFetched"
          />
          <div v-else class="no-conversation-message">
            <SvgChat class="large-svg-message" />
            <h2 class="info-heading">No conversation selected</h2>
          </div>
        </div>
      </div>
    </template>
  </ContainerPage>
</template>

<script setup lang="ts">
import { onMounted, ref, computed, type PropType, watch } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import ContainerPage from "@/pages/commons/ContainerPage.vue"
import ConversationComponent from "@/components/conversations/ConversationComponent.vue"
import SvgChat from "@/components/svgs/SvgChat.vue"
import type { Conversation, User } from "@/generated/backend"
import { messageCenterConversations } from "@/store/messageStore"
import type { StorageAccessor } from "@/store/createNoteStorage"
import { useRouter } from "vue-router"

const { managedApi } = useLoadingApi()

const props = defineProps({
  user: { type: Object as PropType<User> },
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
  conversationId: { type: Number, required: false },
})

const conversations = ref<Conversation[] | undefined>(undefined)
const currentConversation = ref<Conversation | null>(null)

const router = useRouter()

watch(
  () => props.conversationId,
  async (newId) => {
    if (newId) {
      currentConversation.value =
        await managedApi.restConversationMessageController.getConversation(
          newId
        )
      return
    }
    currentConversation.value = null
  }
)

const fetchData = async () => {
  conversations.value =
    await managedApi.restConversationMessageController.getConversationsOfCurrentUser()

  if (props.conversationId && conversations.value) {
    currentConversation.value =
      conversations.value.find((c) => c.id === Number(props.conversationId)) ||
      null
  }
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
    conversation.subject?.assessmentQuestionInstance?.bareQuestion
      .multipleChoicesQuestion.stem ||
    conversation.subject?.note?.noteTopic?.topicConstructor
  )
}

const conversationPartner = (conversation: Conversation) => {
  if (conversation.conversationInitiator?.name !== props.user?.name) {
    return conversation.conversationInitiator?.name
  }
  if (conversation.subjectOwnership?.circle?.name) {
    return conversation.subjectOwnership.circle.name
  }
  return conversation.subjectOwnership?.ownerName
}

const showSidebarOnMobile = computed(() => !currentConversation.value)
const showMainContentOnMobile = computed(() => currentConversation.value)

const selectConversation = (conversation: Conversation) => {
  router.push({
    name: "messageCenter",
    params: { conversationId: conversation.id },
  })
}

const backToList = () => {
  router.push({ name: "messageCenter" })
}
</script>

<style scoped>
.message-center-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 80px);
  min-height: 400px; /* Ensure a minimum height on smaller screens */
}

.sidebar {
  flex: 0 0 auto;
  overflow-y: auto;
  max-height: 300px; /* Limit height on mobile */
  border-right: 1px solid #e0e0e0; /* Add this line */
}

.main-content {
  flex: 1 1 auto;
  overflow-y: auto;
  min-height: 200px; /* Ensure minimum height for content */
  display: flex;
  flex-direction: column;
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

.no-conversation-message {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.large-svg-message {
  width: 120px; /* Adjust this value to make the SVG twice as big */
  height: 120px; /* Adjust this value to make the SVG twice as big */
  margin-bottom: 20px;
  opacity: 0.5; /* This makes the SVG half-tone */
}

.list-group-item.active {
  background-color: #007bff;
  color: white;
  border-color: #007bff;
}

.mobile-back-button {
  display: none;
  padding: 10px;
  cursor: pointer;
  background-color: #f8f9fa;
  border-bottom: 1px solid #e0e0e0;
  flex: 0 0 auto;
}

.mobile-back-button:hover {
  background-color: #e9ecef;
}

@media (max-width: 767px) {
  .hide-on-mobile {
    display: none !important;
  }

  .mobile-back-button {
    display: block;
  }

  .sidebar, .main-content {
    flex: 0 0 100%;
    max-width: 100%;
  }
}
</style>

ai-chat
