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
import useLoadingApi from "@/managedApi/useLoadingApi"
import ContainerPage from "@/pages/commons/ContainerPage.vue"
import { onMounted, ref } from "vue"

const { managedApi } = useLoadingApi()
const emits = defineEmits(["user-updated"])

const formData = ref<User | undefined>()
const errors = ref<Record<string, string>>({})

const fetchData = async () => {
  formData.value = await managedApi.services.getUserProfile()
}

const processForm = async () => {
  if (!formData.value) return
  const updated = await managedApi.services
    .updateUser({ path: { user: formData.value.id }, body: formData.value })
    .catch((err) => {
      errors.value = err as Record<string, string>
      return undefined
    })
  emits("user-updated", updated)
}

onMounted(() => fetchData())
</script>
