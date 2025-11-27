<template>
  <ManageModelInner
    v-bind="{ modelList, selectedModels }"
    v-if="modelList && selectedModels"
    @save="save"
  />
  <ContentLoader v-else />
</template>
<script lang="ts" setup>
import { onMounted, ref } from "vue"
import type { GlobalAiModelSettings } from "@generated/backend"
import {
  AiController,
  GlobalSettingsController,
} from "@generated/backend/sdk.gen"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import ManageModelInner from "./ManageModelInner.vue"
import { apiCallWithLoading } from "@/managedApi/clientSetup"

const modelList = ref<string[] | undefined>(undefined)
const selectedModels = ref<GlobalAiModelSettings | undefined>(undefined)

onMounted(async () => {
  const [modelListRes, selectedModelRes] = await Promise.all([
    apiCallWithLoading(() => AiController.getAvailableGptModels({})),
    apiCallWithLoading(() =>
      GlobalSettingsController.getCurrentModelVersions({})
    ),
  ])
  const { data: models, error: modelsError } = modelListRes
  const { data: modelsSettings, error: settingsError } = selectedModelRes
  if (!modelsError && models) {
    modelList.value = models
  }
  if (!settingsError && modelsSettings) {
    selectedModels.value = modelsSettings
  }
})

const save = async (settings: GlobalAiModelSettings) => {
  const { data: updatedSettings, error } =
    await GlobalSettingsController.setCurrentModelVersions({
      body: settings,
    })
  if (!error && updatedSettings) {
    selectedModels.value = updatedSettings
  }
}
</script>
