<template>
  <LoadingThinBar v-if="apiStatus.states.length > 0" />
</template>

<script lang="ts">
import LoadingThinBar from "@/components/commons/LoadingThinBar.vue"
import type { ApiStatus } from "@/managedApi/ManagedApi"
import type { PropType } from "vue"
import { defineComponent } from "vue"

export default defineComponent({
  props: {
    apiStatus: { type: Object as PropType<ApiStatus>, required: true },
  },
  components: {
    LoadingThinBar,
  },
})
</script>
