<template>
  <form @submit.prevent="submit">
    <TextInput
      v-model="name"
      scope-name="notebookGroup"
      field="name"
      title="Group name"
      :error-message="errorMessage"
      autofocus
    />
    <button type="submit" class="daisy-btn daisy-btn-primary daisy-mt-3">
      Create notebook group
    </button>
  </form>
</template>

<script setup lang="ts">
import { ref } from "vue"
import { NotebookGroupController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { toOpenApiError } from "@/managedApi/openApiError"
import TextInput from "@/components/form/TextInput.vue"

const props = defineProps<{
  close: () => void
}>()

const emit = defineEmits<{
  (e: "created"): void
}>()

const name = ref("")
const errorMessage = ref<string | undefined>(undefined)

const submit = async () => {
  errorMessage.value = undefined
  const trimmed = name.value.trim()
  if (!trimmed) {
    errorMessage.value = "Name is required"
    return
  }
  const { error } = await apiCallWithLoading(() =>
    NotebookGroupController.createGroup({
      body: { name: trimmed },
    })
  )
  if (!error) {
    emit("created")
    props.close()
  } else {
    const errorObj = toOpenApiError(error)
    errorMessage.value =
      errorObj.errors?.name ?? errorObj.message ?? "Could not create group"
  }
}
</script>
