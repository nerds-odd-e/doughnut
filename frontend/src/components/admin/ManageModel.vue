<template>
  <ManageModelInner
    v-bind="{ modelList, selectedModels }"
    v-if="modelList && selectedModels"
    @save="save"
  />
  <LoadingPage v-else />
</template>
<script lang="ts" setup>
import { ref, onMounted } from "vue";
import ManageModelInner from "./ManageModelInner.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import LoadingPage from "../../pages/commons/LoadingPage.vue";

const { api } = useLoadingApi();
const modelList = ref<string[] | undefined>(undefined);
const selectedModels = ref<Generated.GlobalAiModelSettings | undefined>(
  undefined,
);

onMounted(() => {
  Promise.all([
    api.ai.getManageModel(),
    api.settings.getManageModelSelected(),
  ]).then((results) => {
    const [modelListRes, selectedModelRes] = results;
    modelList.value = modelListRes;
    selectedModels.value = selectedModelRes;
  });
});

const save = async (settings: Generated.GlobalAiModelSettings) => {
  selectedModels.value = await api.settings.setManageModelSelected(settings);
};
</script>
