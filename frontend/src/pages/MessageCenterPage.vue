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
              <div>{{ conversation.assessmentQuestionInstance?.bareQuestion.multipleChoicesQuestion.stem }}</div>
              <div>{{ conversationPartner(conversation) }}</div>
            </li>
          </ul>
        </div>

        <div class="col-md-9 main-content">
          <div class="px-3 py-3 conversations" v-if="conversationDetailThreads">
            <div v-for="thread in conversationDetailThreads" :key="thread.id" class="d-flex mb-3" :class="{ 'justify-content-end': isCurrentUser(thread.conversationDetailInitiator?.id || 0) }">
              <div class="card py-2 px-3" :class="[isCurrentUser(thread.conversationDetailInitiator?.id || 0) ? 'text-bg-dark': 'bg-light', thread.conversationDetailInitiator?.id === undefined ? 'ai-chat' : '']">
                <template v-if="thread.conversationDetailInitiator?.id === undefined">
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
                <div class="col-md-1">
                  <button
                  type="button"
                  value="Chat"
                  id="ask-ai"
                  class="btn float-btn btn-secondary"
                  @click="askAI">AI</button>
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
import { onMounted, ref, toRefs } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import ContainerFluidPage from "@/pages/commons/ContainerFluidPage.vue"
import type {
  Conversation,
  User,
  ConversationDetail,
} from "@/generated/backend"

const { managedApi } = useLoadingApi()

const props = defineProps({
  user: { type: Object as PropType<User> },
})
const { user } = toRefs(props)

const conversations = ref<Conversation[] | undefined>(undefined)
const conversationThreadId = ref(0)
const conversationDetailThreads = ref<ConversationDetail[] | undefined>(
  undefined
)
const message = ref("")

const formatMessage = (message: string) => {
  return message.replace(/^"|"$/g, "").trim()
}

const isCurrentUser = (id: number): boolean => {
  return id === user?.value?.id
}

const fetchData = async () => {
  conversations.value =
    await managedApi.restFeedbackController.getFeedbackThreadsForUser()
}

const handleSendMessage = async () => {
  await managedApi.restFeedbackController.sendMessage(
    conversationThreadId.value,
    message.value
  )
  message.value = ""
  await fetchThreadsForConversation(conversationThreadId.value)
}

const fetchThreadsForConversation = async (conversationId: number) => {
  conversationDetailThreads.value =
    await managedApi.restFeedbackController.getMessageThreadsForConversation(
      conversationId
    )
  conversationThreadId.value = conversationId
}

const askAI = async () => {
  await managedApi.restAiController
    .getCompletionAiOpinion(conversationThreadId.value)
    .then(() => {
      fetchThreadsForConversation(conversationThreadId.value)
    })
}

onMounted(() => {
  fetchData()
})

const conversationPartner = (conversation: Conversation) => {
  if (conversation.conversationInitiator?.name !== props.user?.name) {
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
