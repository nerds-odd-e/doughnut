<template>
  <ContainerPage
    v-bind="{
      contentExists: true,
      title: 'Feedback',
    }"
  >
    <h2>There is no feedback currently.</h2>
    <table class="feedback-table mt-2">
      <thead>
      <tr>
        <th>Question</th>
        <th>Name</th>
        <th>Feedback</th>
        <th></th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="conversation in conversations" :key="conversation.id">
        <td>{{conversation.quizQuestionAndAnswer?.quizQuestion.multipleChoicesQuestion.stem}}</td>
        <td>{{conversation.conversationInitiator?.name}}</td>
        <td>{{conversation.message}}</td>
        <td>
          <router-link :to="{ name: 'feedbackConversation',  params: { conversationId: '123' } }">View chat</router-link>
        </td>
      </tr>
      </tbody>
    </table>
  </ContainerPage>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue"
const { managedApi } = useLoadingApi()
import useLoadingApi from "@/managedApi/useLoadingApi"
import ContainerPage from "@/pages/commons/ContainerPage.vue"
import { Conversation } from "@/generated/backend"

const conversations = ref<Conversation[] | undefined>(undefined)

const fetchData = async () => {
  conversations.value =
    await managedApi.restFeedbackController.getFeedbackThreadsForUser()
  // notebooks.value = res.notebooks
  // subscriptions.value = res.subscriptions
}
onMounted(() => {
  fetchData()
})
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

