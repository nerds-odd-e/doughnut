<template>
  <button @click="recreateAllAssistants">Recreate Default Assistant</button>
  <div v-if="assistants">
    <div v-for="(assistant, assistantId) in assistants" :key="assistantId">
      <label
        >{{ assistantId }}
        <input type="text" :value="assistant" />
      </label>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref } from "vue"
import useLoadingApi from "@/managedApi/useLoadingApi"

const { managedApi } = useLoadingApi()
const assistants = ref<Record<string, string> | undefined>(undefined)

const recreateAllAssistants = async () => {
  assistants.value =
    await managedApi.restAiAssistantCreationController.recreateDefaultAssistant()
}
</script>
