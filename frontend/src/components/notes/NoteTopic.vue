<template>
  {{ topic }}
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  props: {
    topic: { type: String, required: true },
  },
});
</script>
