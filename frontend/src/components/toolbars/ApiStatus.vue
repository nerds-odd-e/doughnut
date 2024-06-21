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
import LastErrorMessage from "@/components/commons/LastErrorMessage.vue"
import LoadingThinBar from "@/components/commons/LoadingThinBar.vue"
import { ApiStatus } from "@/managedApi/ManagedApi"
import { PropType, defineComponent } from "vue"

export default defineComponent({
  props: {
    apiStatus: { type: Object as PropType<ApiStatus>, required: true },
  },
  emits: ["clearErrorMessage"],
  components: {
    LoadingThinBar,
    LastErrorMessage,
  },
})
</script>
