<template>
  <ContainerPage
    v-bind="{
      title: 'Welcome, new user. Please create your profile',
    }"
  >
    <form @submit.prevent.once="processForm">
      <TextInput
        scope-name="user"
        field="name"
        v-model="formData.name"
        :autofocus="true"
        :error-message="errors.name"
        placeholder="Nickname"
      />
      <input type="submit" value="Submit" class="daisy-btn daisy-btn-primary" />
    </form>
  </ContainerPage>
</template>

<script setup lang="ts">
import { ref } from "vue"
import type { User } from "@generated/backend"
import { createUser } from "@generated/backend/sdk.gen"
import { toOpenApiError } from "@/managedApi/openApiError"
import TextInput from "@/components/form/TextInput.vue"
import ContainerPage from "./commons/ContainerPage.vue"

const formData = ref({ name: undefined as undefined | string } as User)
const errors = ref<Record<string, string>>({})

const emits = defineEmits(["updateUser"])

const processForm = async () => {
  const { data: newUser, error } = await createUser({
    body: formData.value,
  })
  if (!error) {
    emits("updateUser", newUser!)
  } else {
    // Error is handled by global interceptor (toast notification)
    // Extract field-level errors if available (for 400 validation errors)
    const errorObj = toOpenApiError(error)
    errors.value = errorObj.errors || {
      name: errorObj.message || "Failed to create user",
    }
  }
}
</script>
