<template>
  <div data-testid="correction-source">
    <template v-if="correction.sourceQuote">
      <blockquote>{{ correction.sourceQuote }}</blockquote>
      <a class="daisy-link" :href="quotedPassageLink" target="_blank" rel="noopener noreferrer">{{ correction.sourceUrl }}</a>
    </template>
    <p v-else>No source was found.</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue"
import type { ConversationNoteCorrection } from "@generated/donut-backend-api"

const props = defineProps<{ correction: ConversationNoteCorrection }>()

const quotedPassageLink = computed(() => {
  const address = props.correction.sourceUrl!
  const quote = encodeURIComponent(props.correction.sourceQuote!).replace(
    /-/g,
    "%2D"
  )
  return `${address}${address.includes("#") ? "" : "#"}:~:text=${quote}`
})
</script>
