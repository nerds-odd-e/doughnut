<template>
  <LoadingThinBar v-if="apiStatus.states.length > 0" />
  <LastErrorMessage
    v-for="error in apiStatus.errors"
    :error="error"
    @close="$emit('clearErrorMessage', error.id)"
    :key="error.id"
  />
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { ApiStatus } from "@/managedApi/ManagedApi";
import LoadingThinBar from "@/components/commons/LoadingThinBar.vue";
import LastErrorMessage from "@/components/commons/LastErrorMessage.vue";

export default defineComponent({
  props: {
    apiStatus: { type: Object as PropType<ApiStatus>, required: true },
  },
  emits: ["clearErrorMessage"],
  components: {
    LoadingThinBar,
    LastErrorMessage,
  },
});
</script>
