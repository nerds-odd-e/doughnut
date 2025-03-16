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
      <input type="submit" value="Submit" class="daisy:btn daisy:btn-primary" />
    </form>
  </ContainerPage>
</template>

<script setup lang="ts">
import { ref } from "vue"
import type { User } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import TextInput from "@/components/form/TextInput.vue"
import ContainerPage from "./commons/ContainerPage.vue"

const { managedApi } = useLoadingApi()

const formData = ref({ name: undefined as undefined | string } as User)
const errors = ref({ name: undefined as undefined | string })

const emits = defineEmits(["updateUser"])

const processForm = async () => {
  try {
    const user = await managedApi.restUserController.createUser(formData.value)
    emits("updateUser", user)
  } catch (err: unknown) {
    if (err instanceof Error) {
      errors.value = { name: err.message }
    } else {
      errors.value = { name: String(err) }
    }
  }
}
</script>
