<template>
  <PopButton
    btn-class="btn btn-light"
    title="View Certificate"
  >
    <CertificatePopup
      :assessment-attempt="assessmentAttempt"
      :notebook-id="certificate?.notebook?.id"
    >
    </CertificatePopup>
  </PopButton>
</template>

<script setup lang="ts">
import { PropType } from "vue"
// import useLoadingApi from "@/managedApi/useLoadingApi"
import { Certificate, AssessmentAttempt } from "@/generated/backend"

// const { managedApi } = useLoadingApi()
defineProps({
  assessmentAttempt: {
    type: Object as PropType<AssessmentAttempt>,
    required: true,
  },
  certificate: {
    type: Object as PropType<Certificate>,
    required: false,
  },
})
</script>
