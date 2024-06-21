<template>
  <ManageModelInner
    v-bind="{ modelList, selectedModels }"
    v-if="modelList && selectedModels"
    @save="save"
  />
  <ContentLoader v-else />
</template>
<script lang="ts" setup>
import { GlobalAiModelSettings } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { onMounted, ref } from "vue"

const { managedApi } = useLoadingApi()
const modelList = ref<string[] | undefined>(undefined)
const selectedModels = ref<GlobalAiModelSettings | undefined>(undefined)

onMounted(() => {
  Promise.all([
    managedApi.restAiController.getAvailableGptModels(),
    managedApi.restGlobalSettingsController.getCurrentModelVersions(),
  ]).then((results) => {
    const [modelListRes, selectedModelRes] = results
    modelList.value = modelListRes
    selectedModels.value = selectedModelRes
  })
})

const save = async (settings: GlobalAiModelSettings) => {
  selectedModels.value =
    await managedApi.restGlobalSettingsController.setCurrentModelVersions(
      settings,
    )
}
</script>
