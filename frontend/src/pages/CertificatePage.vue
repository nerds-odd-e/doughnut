<template>
  <div v-if="notebook && user" class="certificate-frame">
    <div class="certificate-container">
      <span>This to certificate that</span>
      <span class="reciever-name">{{ user.name }}</span>
      <p class="certificate-detail">
        <span>by completing the qualifications, </span
        ><span
          >is granted the Certified
          {{ notebook.headNote.noteTopic.topicConstructor }}</span
        >
      </p>
      <div class="date-container">
        <span>on</span>
        <span class="date">{{ issueDate }}</span>
        <span>, and expiring on</span>
        <span data-cy="expired-date" class="date">{{ expiredDate }}</span>
      </div>
      <div class="signature-section">
        <div class="signature">
          <span>{{ notebook.notebookSettings.certifiedBy }}</span>
          <span>Content Creator</span>
        </div>
        <div class="signature">
          <span>Terry</span>
          <span>Odd-e</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { Notebook, User } from "@/generated/backend"

const props = defineProps({
  notebookId: { type: Number, required: true },
})
const { managedApi } = useLoadingApi()
const notebook = ref<Notebook | undefined>(undefined)
const user = ref<User | undefined>(undefined)
const issueDate = ref("")
const expiredDate = ref("")

function addDays(date: Date, days: number = 0): Date {
  const result = new Date(date)
  result.setDate(result.getDate() + days)
  return result
}

const fetchData = async () => {
  notebook.value = await managedApi.restNotebookController.get(props.notebookId)
  user.value = await managedApi.restUserController.getUserProfile()
}
const prepare = async () => {
  const now = new Date()
  const theMonth = `0${now.getMonth() + 1}`.substr(-2)
  const theDate = `0${now.getDate()}`.substr(-2)
  issueDate.value = `${now.getFullYear()}-${theMonth}-${theDate}`
  const expDate = addDays(now, notebook.value?.notebookSettings.untilCertExpire)
  const theExpiredMonth = `0${expDate.getMonth() + 1}`.substr(-2)
  const theExpiredDate = `0${expDate.getDate()}`.substr(-2)
  expiredDate.value = `${expDate.getFullYear()}-${theExpiredMonth}-${theExpiredDate}`
}
onMounted(() => {
  fetchData().then(() => prepare())
})
</script>

<style lang="scss" scoped>
.certificate-frame {
  border: solid 16px lightskyblue;
  margin: 32px 64px;
}
.certificate-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 80vh;
  row-gap: 32px;
  border: solid 2px lightskyblue;
  margin: 8px;
}
.certificate-detail {
  display: flex;
  flex-direction: column;
  text-align: center;
}
.reciever-name {
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
</style>