<template>
  <ContainerPage v-bind="{ contentLoaded: reviewing !== undefined }">
    <ReviewWelcome v-if="!!reviewing" v-bind="{ reviewing }" />
  </ContainerPage>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import timezoneParam from "@/managedApi/window/timezoneParam"
import useLoadingApi from "@/managedApi/useLoadingApi"
import ReviewWelcome from "@/components/review/ReviewWelcome.vue"
import ContainerPage from "./commons/ContainerPage.vue"
import type { RecallStatus } from "@/generated/backend"

const { managedApi } = useLoadingApi()
const reviewing = ref<RecallStatus | undefined>(undefined) // Replace 'any' with proper type from your API

const fetchData = async () => {
  reviewing.value = await managedApi.restRecallsController.overview(
    timezoneParam()
  )
}

onMounted(() => {
  fetchData()
})
</script>
