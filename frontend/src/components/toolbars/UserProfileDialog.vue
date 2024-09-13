<template>
  <ContainerPage
    v-bind="{
      contentExists: formData !== undefined,
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
          :errors="errors.name"
        />
        <TextInput
          scope-name="user"
          field="dailyNewNotesCount"
          v-model="formData.dailyNewNotesCount"
          :errors="errors.dailyNewNotesCount"
        />
        <TextInput
          scope-name="user"
          field="spaceIntervals"
          v-model="formData.spaceIntervals"
          :errors="errors.spaceIntervals"
        />
        <CheckInput
          scope-name="user"
          field="aiQuestionTypeOnlyForReview"
          v-model="formData.aiQuestionTypeOnlyForReview"
          :errors="getErrorObject('aiQuestionTypeOnlyForReview')"
        />
        <input type="submit" value="Submit" class="btn btn-primary" />
      </form>
    </div>
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent, ref, computed } from "vue"
import CheckInput from "@/components/form/CheckInput.vue"
import TextInput from "@/components/form/TextInput.vue"
import { User } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import ContainerPage from "@/pages/commons/ContainerPage.vue"

export default defineComponent({
  components: { ContainerPage, TextInput, CheckInput },
  emits: ["user-updated"],
  setup(_, { emit }) {
    const { managedApi } = useLoadingApi()
    const formData = ref<User | undefined>(undefined)
    const errors = ref<Record<string, string>>({})

    const getErrorObject = computed(() => (field: string) => {
      const error = errors.value[field]
      return error ? { [field]: error } : undefined
    })

    const fetchData = async () => {
      formData.value = await managedApi.restUserController.getUserProfile()
    }

    const processForm = async () => {
      if (!formData.value) return
      try {
        const updated = await managedApi.restUserController.updateUser(
          formData.value.id,
          formData.value
        )
        emit("user-updated", updated)
      } catch (err) {
        if (typeof err === "object" && err !== null) {
          errors.value = err as Record<string, string>
        } else {
          console.error("Unexpected error format:", err)
        }
      }
    }

    fetchData()

    return {
      formData,
      errors,
      getErrorObject,
      processForm,
    }
  },
})
</script>