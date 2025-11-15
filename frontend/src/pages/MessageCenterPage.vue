<template>
  <ContainerPage
    v-bind="{
      fullHeight: true,
      contentLoaded: conversations !== undefined,
      title: 'Message Center',
    }"
  >
    <h2 v-if="!conversations?.length" class="info-heading">
      There is no conversation currently.
    </h2>

    <template v-else>
      <div class="message-center-container">
        <div class="sidebar" :class="{ 'hide-on-mobile': !showSidebarOnMobile }">
          <ul class="daisy-menu">
            <li
              v-for="conversation in conversations"
              :key="conversation.id"
              class="daisy-menu-item"
              :class="{ 'daisy-active': conversationId === conversation.id }"
              @click="selectConversation(conversation)"
            >
              <div>{{ conversationTopic(conversation) }}</div>
              <div>{{ conversationPartner(conversation) }}</div>
            </li>
          </ul>
        </div>

        <div class="conversation" :class="{ 'hide-on-mobile': !showMainContentOnMobile }">
          <div class="mobile-back-button" @click="backToList">
            <span>&larr; Back to conversations</span>
          </div>
          <ConversationComponent
            v-if="currentConversation && user"
            :conversation="currentConversation"
            :user="user"
            :storageAccessor="storageAccessor"
            @conversation-fetched="handleConversationFetched"
            @conversation-changed="handleConversationChanged"
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
import {
  onMounted,
  ref,
  computed,
  type PropType,
  watch,
  inject,
  type Ref,
} from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import ContainerPage from "@/pages/commons/ContainerPage.vue"
import ConversationComponent from "@/components/conversations/ConversationComponent.vue"
import SvgChat from "@/components/svgs/SvgChat.vue"
import type { Conversation, User } from "@generated/backend"
import { messageCenterConversations } from "@/store/messageStore"
import type { StorageAccessor } from "@/store/createNoteStorage"
import { useRouter } from "vue-router"

const { managedApi } = useLoadingApi()

const props = defineProps({
  storageAccessor: {
    type: Object as PropType<StorageAccessor>,
    required: true,
  },
  conversationId: { type: Number, required: false },
})

const user = inject<Ref<User | undefined>>("currentUser")

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
    conversation.subject?.assessmentQuestionInstance?.multipleChoicesQuestion
      .stem || conversation.subject?.note?.noteTopology?.titleOrPredicate
  )
}

const conversationPartner = (conversation: Conversation) => {
  if (conversation.conversationInitiator?.name !== user?.value?.name) {
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

const handleConversationChanged = (conversationId: number) => {
  router.push({
    name: "messageCenter",
    params: { conversationId },
  })
}
</script>

<style scoped lang="scss">
.message-center-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.sidebar {
  flex: 0 0 auto;
  overflow-y: auto;
  max-height: 300px;
  border-right: 1px solid #e0e0e0;
}

.conversation {
  flex: 1 1 auto;
  overflow-y: auto;
  min-height: 200px;
  display: flex;
  flex-direction: column;
}

@media (min-width: theme('screens.md')) {
  .message-center-container {
    flex-direction: row;
  }

  .sidebar {
    flex: 0 0 25%;
    max-height: none;
  }

  .conversation {
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
  color: #808080;
}

.no-conversation-message {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.large-svg-message {
  width: 120px;
  height: 120px;
  margin-bottom: 20px;
  opacity: 0.5;
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

@media (max-width: theme('screens.md')) {
  .hide-on-mobile {
    display: none !important;
  }

  .mobile-back-button {
    display: block;
  }

  .sidebar, .conversation {
    flex: 0 0 100%;
    max-width: 100%;
  }
}
</style>

ai-chat
