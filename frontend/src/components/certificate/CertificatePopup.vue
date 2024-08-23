<template>
  <div v-if="certificate" class="certificate-frame">
    <div class="certificate-container">
      <span>This to certificate that</span>
      <span class="receiver-name">{{ certificate.user?.name }}</span>
      <p class="certificate-detail">
        <span>by completing the qualifications, </span>
        <span>is granted the Certified
          <span class="certificate-name"> {{ certificate.notebook?.headNote.noteTopic.topicConstructor  }}</span>
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
          <span class="signature-creator-name">{{ certificate.creatorName }}</span>
          <span>Content Creator</span>
        </div>
        <div class="signature">
          <span class="signature-issuer-name">Terry</span>
          <span>Odd-e</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, PropType } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { AssessmentAttempt, Certificate } from "@/generated/backend"
const props = defineProps({
  notebookId: { type: Number, required: true },
  assessmentAttempt: {
    type: Object as PropType<AssessmentAttempt>,
    required: true,
  },
})
const { managedApi } = useLoadingApi()
const certificate = ref<Certificate | undefined>(undefined)

const issueDate = computed(() => certificate.value?.startDate.split("T")[0])
const expiredDate = computed(() => certificate.value?.expiryDate.split("T")[0])
const fetchData = async () => {
  certificate.value = await managedApi.restCertificateController.getCertificate(
    props.notebookId
  )
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
.signature-creator-name {
  font-style: italic;
}
.signature-issuer-name {
  font-style: italic;
}
</style>
