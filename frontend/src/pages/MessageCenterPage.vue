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
            <li v-for="conversation in conversations" :key="conversation.id" class="list-group-item list-group-item-action" @click="setInitialFeedback(conversation.message || '')">
              <div align="right">
                <AgreeButton :conversation="conversation" data-testid="AgreeButton"></AgreeButton>
                <DeclineButton :conversation="conversation" data-testid="AgreeButton"></DeclineButton>
              </div>
              <div>{{ conversation.assessmentQuestionInstance?.bareQuestion.multipleChoicesQuestion.stem }}</div>
              <div>{{ conversationPartner(conversation) }}</div>
            </li>
          </ul>
        </div>

        <div class="col-md-9 main-content">
          <div class="p-4">
            <div v-if="feedback">{{ feedback }}</div>
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
import type { Conversation, User } from "@/generated/backend"
import AgreeButton from "@/components/toolbars/AgreeButton.vue"
import DeclineButton from "@/components/toolbars/DeclineButton.vue"

const { managedApi } = useLoadingApi()

const props = defineProps({
  user: { type: Object as PropType<User> },
})

const conversations = ref<Conversation[] | undefined>(undefined)
const feedback = ref("")

const fetchData = async () => {
  conversations.value =
    await managedApi.restFeedbackController.getFeedbackThreadsForUser()
}

const setInitialFeedback = (message: string) => {
  feedback.value = message.replace(/^"|"$/g, "").trim()
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

