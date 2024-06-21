<template>
  <ToolbarCover class="last-error-message" @click="$emit('close')">
    {{ error.message }}
  </ToolbarCover>
</template>

<script lang="ts">
import { ApiError } from "@/managedApi/ManagedApi"
import { PropType, defineComponent } from "vue"
import ToolbarCover from "./ToolbarOver.vue"

export default defineComponent({
  props: {
    error: { type: Object as PropType<ApiError>, required: true },
  },
  components: {
    ToolbarCover,
  },
  emits: ["close"],
})
</script>
