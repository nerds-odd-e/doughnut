<template>
  <h2>Give Feedback</h2>
  <p>
    <i>
      Please tell us if there is anything wrong with the question.
    </i>
  </p>
  <TextArea
    field="comment"
    v-model="feedback"
    placeholder="Give feedback about the question"
    :rows="5"
  />
  <div>
    <button class="btn btn-success" @click="submitFeedback">
      Submit
    </button>
  </div>
</template>

<script setup lang="ts">
import type { AssessmentQuestionInstance } from "@generated/backend"
import { ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi.ts"

const { managedApi } = useLoadingApi()
const props = defineProps<{
  question: AssessmentQuestionInstance
}>()

const feedback = ref<string>("")

const emit = defineEmits(["submitted"])

async function submitFeedback() {
  await managedApi.services.startConversationAboutAssessmentQuestion({
    path: { assessmentQuestion: props.question.id },
    body: feedback.value,
  })

  emit("submitted")
}
</script>
