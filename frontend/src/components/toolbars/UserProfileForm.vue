<template>
  <ContainerPage
    v-bind="{
      contentLoaded: formData !== undefined,
      title: 'Edit User Setting',
    }"
  >
    <div v-if="formData">
      <form @submit.prevent.once="processForm">
        <TextInput
          scope-name="user"
          field="name"
          v-model="formData.name"
          :autofocus="true"
          :error-message="errors.name"
        />
        <TextInput
          scope-name="user"
          field="dailyAssimilationCount"
          v-model="formData.dailyAssimilationCount"
          :error-message="errors.dailyAssimilationCount"
        />
        <TextInput
          scope-name="user"
          field="spaceIntervals"
          v-model="formData.spaceIntervals"
          :error-message="errors.spaceIntervals"
        />
        <section class="my-4">
          <h3 class="mb-2 text-base font-semibold">Batch questions</h3>
          <div class="text-sm" data-testid="batch-question-schedule">
            <span class="font-medium">Next batch question generation: </span>
            <span v-if="formattedNextScheduledAt">
              {{ formattedNextScheduledAt }}
            </span>
            <span v-else-if="batchSchedule">
              No batch question generation is scheduled yet
            </span>
            <span v-else>Loading...</span>
          </div>
        </section>
        <input type="submit" value="Submit" class="daisy-btn daisy-btn-primary" />
      </form>
    </div>
  </ContainerPage>
</template>

<script setup lang="ts">
import TextInput from "@/components/form/TextInput.vue"
import type {
  QuestionGenerationBatchUserScheduleDto,
  User,
} from "@generated/doughnut-backend-api"
import { UserController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import ContainerPage from "@/pages/commons/ContainerPage.vue"
import { computed, onMounted, ref } from "vue"
import { toOpenApiError } from "@/managedApi/openApiError"

const emits = defineEmits(["user-updated"])

const formData = ref<User | undefined>()
const batchSchedule = ref<QuestionGenerationBatchUserScheduleDto | undefined>()
const errors = ref<Record<string, string>>({})

const formattedNextScheduledAt = computed(() => {
  if (!batchSchedule.value?.nextScheduledAt) return undefined
  return new Date(batchSchedule.value.nextScheduledAt).toLocaleString()
})

const fetchData = async () => {
  const { data, error } = await UserController.getUserProfile({})
  if (!error && data) {
    formData.value = data
  }
}

const fetchBatchSchedule = async () => {
  const { data, error } =
    await UserController.getQuestionGenerationBatchSchedule({})
  if (!error) {
    batchSchedule.value = data
  }
}

const processForm = async () => {
  if (!formData.value) return
  const userData = formData.value
  const { data: updatedUser, error } = await apiCallWithLoading(() =>
    UserController.updateUser({
      path: { user: userData.id },
      body: userData,
    })
  )
  if (error) {
    // Error is handled by global interceptor (toast notification)
    // Extract field-level errors if available (for 400 validation errors)
    const errorObj = toOpenApiError(error)
    errors.value = errorObj.errors || {}
  } else {
    errors.value = {}
    emits("user-updated", updatedUser)
  }
}

onMounted(() => {
  fetchData()
  fetchBatchSchedule()
})
</script>
