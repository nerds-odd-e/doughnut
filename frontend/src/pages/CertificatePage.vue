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
        <span class="date">2015-07-31</span>
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
const d = new Date()
const theMonth = `0${d.getMonth() + 1}`.substr(-2)
const theDate = `0${d.getDate()}`.substr(-2)
const issueDate = `${d.getFullYear()}-${theMonth}-${theDate}`

const fetchData = async () => {
  notebook.value = await managedApi.restNotebookController.get(props.notebookId)
  user.value = await managedApi.restUserController.getUserProfile()
}
onMounted(() => {
  fetchData()
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