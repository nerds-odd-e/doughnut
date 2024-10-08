<template>
  <ContainerPage
    v-bind="{
      contentExists: true,
      title: 'Message Center',
    }"
  >
    <h2 v-if="!conversations?.length">There is no feedback currently.</h2>

    <table v-if="conversations?.length" class="feedback-table mt-2">
      <thead>
      <tr>
        <th>Question</th>
        <th>Name</th>
        <th>Feedback</th>
        <th>Marker</th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="conversation in conversations" :key="conversation.id">
        <td>{{conversation.assessmentQuestionInstance?.bareQuestion.multipleChoicesQuestion.stem}}</td>
        <td>{{conversationPartner(conversation)}}</td>
        <td>{{conversation.message}}</td>
        <td>
          <AgreeButton></AgreeButton>
        </td>
      </tr>
      </tbody>
    </table>
  </ContainerPage>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { onMounted, ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import ContainerPage from "@/pages/commons/ContainerPage.vue"
import type { Conversation, User } from "@/generated/backend"
import AgreeButton from "@/components/toolbars/AgreeButton.vue"

const { managedApi } = useLoadingApi()

const props = defineProps({
  user: { type: Object as PropType<User> },
})

const conversations = ref<Conversation[] | undefined>(undefined)

const fetchData = async () => {
  conversations.value =
    await managedApi.restFeedbackController.getFeedbackThreadsForUser()
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

