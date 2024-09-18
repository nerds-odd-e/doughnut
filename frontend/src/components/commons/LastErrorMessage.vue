<template>
  <ToolbarCover class="last-error-message" @click="$emit('close')">
    {{ error.message }}
  </ToolbarCover>
</template>

<script lang="ts">
import type { ApiError } from "@/managedApi/ManagedApi"
import type { PropType } from "vue"
import { defineComponent } from "vue"
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
