<template>
  <p class="note-short-details">{{ shortDetails }}</p>
</template>

<script lang="ts">
import { defineComponent } from "vue";

function truncateString(str: string, maxLength = 50): string {
  // Parse the input string as HTML using DOMParser
  const parser = new DOMParser();
  const doc = parser.parseFromString(str, "text/html");

  // Extract the plain text content from the parsed HTML
  const plainText = doc.body.textContent || "";

  if (plainText.length > maxLength) {
    return `${plainText.substring(0, maxLength - 3)}...`;
  }
  return plainText;
}

export default defineComponent({
  props: { details: String },
  computed: {
    shortDetails() {
      return truncateString(this.details || "");
    },
  },
});
</script>
