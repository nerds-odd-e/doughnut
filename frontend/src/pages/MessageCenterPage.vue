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
              <div align="right">
                <AgreeAndDeclineButton :conversation="conversation"></AgreeAndDeclineButton>
              </div>
              <div>{{ conversation.assessmentQuestionInstance?.bareQuestion.multipleChoicesQuestion.stem }}</div>
              <div>{{ conversationPartner(conversation) }}</div>
            </li>
          </ul>
        </div>

        <div class="col-md-9 main-content">
          <div class="p-4">
            <div v-if="conversationDetailThreads">
              <div v-for="thread in conversationDetailThreads" :key="thread.id">
                {{ formatMessage(thread.message) }}
              </div>

              <form class="chat-input-container" @submit.prevent="handleSendMessage()">
                <textarea name="Description" v-model="message" />
                <input
                  type="submit"
                  value="Send"
                  id="chat-button"
                  class="btn float-btn btn-secondary"
                />
                <button
                type="button"
                value="Chat"
                id="ask-ai"
                class="btn float-btn btn-secondary"
                @click="askAI">AI</button>
              </form>
            </div>
            <h2 v-else>No conversation</h2>
          </div>
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
  ConversationDetail,
} from "@/generated/backend"
import AgreeAndDeclineButton from "@/components/toolbars/AgreeAndDeclineButton.vue"

const { managedApi } = useLoadingApi()

const props = defineProps({
  user: { type: Object as PropType<User> },
})

const conversations = ref<Conversation[] | undefined>(undefined)
const conversationThreadId = ref(0)
const conversationDetailThreads = ref<ConversationDetail[] | undefined>(
  undefined
)
const message = ref("")

const formatMessage = (message: string) => {
  return message.replace(/^"|"$/g, "").trim()
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
.feedback-table {
  border-collapse: collapse;
  width: 100%;
}

.feedback-table th,
.feedback-table td {
  border: 1px solid #dddddd;
  text-align: left;
  padding: 8px;
}

.feedback-table th {
  background-color: #f2f2f2;
}
</style>

