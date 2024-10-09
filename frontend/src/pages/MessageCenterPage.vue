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
            <li v-for="conversation in conversations" :key="conversation.id" class="list-group-item list-group-item-action">
              <div>{{ conversation.assessmentQuestionInstance?.bareQuestion.multipleChoicesQuestion.stem }}</div>
              <div>{{ conversationPartner(conversation) }}</div>
              <div>{{ conversation.message }}</div>
            </li>
          </ul>
        </div>

        <div class="col-md-9 main-content">
          <div class="p-4">
            <h2>No conversation</h2>
            <!-- You can add more content or components here -->
          </div>
        </div>
      </div>
    </template>

    <!-- <table v-if="conversations?.length" class="feedback-table mt-2">
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
    </table> -->
  </ContainerFluidPage>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { onMounted, ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import ContainerFluidPage from "@/pages/commons/ContainerFluidPage.vue"
import type { Conversation, User } from "@/generated/backend"
// import AgreeButton from "@/components/toolbars/AgreeButton.vue"

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

