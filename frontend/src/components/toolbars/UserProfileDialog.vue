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
        <input type="submit" value="Submit" class="daisy-btn daisy-btn-primary" />
      </form>
    </div>
  </ContainerPage>
</template>

<script setup lang="ts">
import TextInput from "@/components/form/TextInput.vue"
import type { User } from "@generated/backend"
import { UserController } from "@generated/backend/sdk.gen"
import ContainerPage from "@/pages/commons/ContainerPage.vue"
import { onMounted, ref } from "vue"
import { toOpenApiError } from "@/managedApi/openApiError"

const emits = defineEmits(["user-updated"])

const formData = ref<User | undefined>()
const errors = ref<Record<string, string>>({})

const fetchData = async () => {
  const { data, error } = await UserController.getUserProfile()
  if (!error && data) {
    formData.value = data
  }
}

const processForm = async () => {
  if (!formData.value) return
  const { data: updatedUser, error } = await UserController.updateUser({
    path: { user: formData.value.id },
    body: formData.value,
  })
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

onMounted(() => fetchData())
</script>
