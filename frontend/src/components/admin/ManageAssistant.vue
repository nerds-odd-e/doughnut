<template>
  <button @click="recreateAllAssistants">Recreate All Assistants</button>
</template>

<script lang="ts" setup>
import { ref } from "vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

const { api } = useLoadingApi();
const assistants = ref<Record<string, string> | undefined>(undefined);

const recreateAllAssistants = async () => {
  assistants.value = await api.settings.recreateAllAssistants();
};
</script>
