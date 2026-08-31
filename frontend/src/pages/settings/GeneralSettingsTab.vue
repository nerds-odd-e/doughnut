<template>
  <div v-if="formData">
    <form @submit.prevent="processForm">
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
      <div class="flex flex-col gap-2">
        <CheckInput
          scope-name="user"
          field="dailyProbeEnabled"
          title="Daily probe"
          v-model="formData.dailyProbeEnabled"
          :error-message="errors.dailyProbeEnabled"
        />
        <p class="text-xs text-base-content/60 leading-snug mt-1">
          Turning this off stops new Daily probes and ends the probe's own trend
          readout.
        </p>
      </div>
      <button
        type="submit"
        class="daisy-btn daisy-btn-primary"
        data-testid="user-settings-submit"
        :disabled="!canSubmit"
      >
        Submit
      </button>
    </form>
  </div>
  <ContentLoader v-else />
</template>

<script setup lang="ts">
import TextInput from "@/components/form/TextInput.vue"
import CheckInput from "@/components/form/CheckInput.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import type { User } from "@generated/donut-backend-api"
import { UserController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { computed, inject, onMounted, ref, type Ref } from "vue"
import { toOpenApiError } from "@/managedApi/openApiError"

const currentUser = inject<Ref<User | undefined>>("currentUser")

type SavedProfile = Pick<
  User,
  "name" | "dailyAssimilationCount" | "dailyProbeEnabled"
>

const snapshotProfile = (user: User): SavedProfile => ({
  name: user.name,
  dailyAssimilationCount: user.dailyAssimilationCount,
  dailyProbeEnabled: user.dailyProbeEnabled,
})

const formData = ref<User | undefined>()
const savedProfile = ref<SavedProfile | undefined>()
const isSubmitting = ref(false)
const errors = ref<Record<string, string>>({})

const setForm = (user: User) => {
  formData.value = user
  savedProfile.value = snapshotProfile(user)
}

const isDirty = computed(() => {
  if (!formData.value || !savedProfile.value) return false
  return (
    formData.value.name !== savedProfile.value.name ||
    String(formData.value.dailyAssimilationCount) !==
      String(savedProfile.value.dailyAssimilationCount) ||
    formData.value.dailyProbeEnabled !== savedProfile.value.dailyProbeEnabled
  )
})

const canSubmit = computed(() => isDirty.value && !isSubmitting.value)

const fetchData = async () => {
  const { data, error } = await UserController.getUserProfile({})
  if (!error && data) {
    setForm(data)
  }
}

const processForm = async () => {
  if (!formData.value || !canSubmit.value) return
  isSubmitting.value = true
  const userData = formData.value
  try {
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
      setForm(updatedUser ?? userData)
      if (currentUser) {
        currentUser.value = updatedUser ?? userData
      }
    }
  } finally {
    isSubmitting.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>
