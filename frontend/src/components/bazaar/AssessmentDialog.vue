<template>
  <h3>Assessment</h3>
  <p>{{ errorMessage }}</p>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  emits: ["closeDialog"],
  props: {
    errorMessage: String,
  },
});
</script>
