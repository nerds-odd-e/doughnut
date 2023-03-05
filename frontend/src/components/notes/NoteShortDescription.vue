<template>
  <p class="note-short-description">{{ shortDescription }}</p>
</template>

<script lang="ts">
import { defineComponent } from "vue";

function truncateString(str: string, maxLength = 50): string {
  if (str.length > maxLength) {
    return `${str.substring(0, maxLength - 3)}...`;
  }
  return str;
}

export default defineComponent({
  props: { description: String },
  computed: {
    shortDescription() {
      return truncateString(this.description || "");
    },
  },
});
</script>
