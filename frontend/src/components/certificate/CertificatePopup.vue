<template>
  <div v-if="notebook && user" class="certificate-frame">
    <div class="certificate-container">
      <span>This to certificate that</span>
      <span class="receiver-name">{{ user.name }}</span>
      <p class="certificate-detail">
        <span>by completing the qualifications, </span>
        <span>is granted the Certified
          <span class="certificate-name"> {{ notebook.headNote.noteTopic.topicConstructor }}</span>
        </span>
      </p>
      <div class="date-container">
        <span>on</span>
        <span class="date">{{ issueDate }}</span>
        <span>, and expiring on</span>
        <span data-cy="expired-date" data-testid="expired-date" class="date">{{ expiredDate }}</span>
      </div>
      <div class="signature-section">
        <div class="signature">
          <span class="signature-name">{{ notebook.creatorId }}</span>
          <span>Content Creator</span>
        </div>
        <div class="signature">
          <span class="signature-name">Terry</span>
          <span>Odd-e</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { AssessmentAttempt, Notebook, User } from "@/generated/backend"
const props = defineProps({
  notebookId: { type: Number, required: true },
})
const { managedApi } = useLoadingApi()
const notebook = ref<Notebook | undefined>(undefined)
const assesmentResult = ref<AssessmentAttempt | undefined>(undefined)
const user = ref<User | undefined>(undefined)
const issueDate = computed(() =>
  formatDate(
    new Date(
      assesmentResult.value ? assesmentResult.value.submittedAt : Date.now()
    )
  )
)
const expiredDate = computed(() =>
  formatDate(
    new Date(
      assesmentResult.value && assesmentResult.value.certificateExpiresAt
        ? assesmentResult.value.certificateExpiresAt
        : Date.now()
    )
  )
)
const fetchData = async () => {
  notebook.value = await managedApi.restNotebookController.get(props.notebookId)
  const allAssesments =
    await managedApi.restAssessmentController.getAssessmentHistory()
  assesmentResult.value = allAssesments
    .filter((a) => a.notebookId === props.notebookId && a.isPass)
    .sort(
      (a, b) =>
        new Date(b.submittedAt).valueOf() - new Date(a.submittedAt).valueOf()
    )[0]
  user.value = await managedApi.restUserController.getUserProfile()
}
const padZero = (num: number): string => {
  return num.toString().padStart(2, "0")
}
const formatDate = (date: Date): string => {
  const theYear = date.getFullYear()
  const theMonth = padZero(date.getMonth() + 1)
  const theDate = padZero(date.getDate())
  return `${theYear}-${theMonth}-${theDate}`
}
onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.certificate-frame {
  border: solid 16px lightskyblue;
  margin: 16px 16px;
}
.certificate-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  row-gap: 16px;
  border: solid 2px lightskyblue;
  margin: 16px;
}
.certificate-detail {
  display: flex;
  flex-direction: column;
  text-align: center;
}
.certificate-name {
  font-weight: 700;
}
.receiver-name {
  font-size: 32px;
  font-weight: 700;
  color: lightskyblue;
}
.date-container {
  display: flex;
  column-gap: 4px;
}
.date {
  color: lightskyblue;
  text-decoration: underline;
}
.signature-section {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  width: 100%;
  padding: 16px;
}
.signature {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.signature-name {
  font-style: italic;
}
</style>
