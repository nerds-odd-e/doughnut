<template>
  <ToolbarCover class="last-error-message" @click="$emit('close')">
    {{ errorMessage }}
  </ToolbarCover>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import ToolbarCover from "./ToolbarOver.vue";

export default defineComponent({
  props: {
    errorMessage: { type: String, required: true },
  },
  components: {
    ToolbarCover,
  },
  emits: ["close"],
});
</script>
