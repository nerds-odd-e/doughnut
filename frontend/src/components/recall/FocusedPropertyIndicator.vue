<template>
  <p v-if="propertyKey" class="focused-property text-sm font-medium mb-2" data-testid="focused-property-indicator">
    Reviewed property:
    <router-link :to="sourceLocation" class="daisy-link" data-testid="reviewed-property-link">{{ propertyKey }}</router-link>
    <span v-if="propertyValue !== undefined" data-testid="reviewed-property-value">:
      <WikiLinkToken :token="propertyValue" :wiki-links="wikiLinks" :last-saved-markdown="lastSavedMarkdown" />
    </span>
  </p>
</template>

<script setup lang="ts">
import { computed } from "vue"
import type { WikiLink } from "@generated/donut-backend-api"
import WikiLinkToken from "@/components/notes/WikiLinkToken.vue"
import {
  notePropertyLocation,
  notePropertyItemLocation,
} from "@/routes/noteShowLocation"
const props = defineProps<{
  noteId: number
  propertyKey?: string
  propertyValue?: string
  wikiLinks: WikiLink[]
  lastSavedMarkdown?: string
}>()
const sourceLocation = computed(() =>
  props.propertyValue === undefined
    ? notePropertyLocation(props.noteId, props.propertyKey!)
    : notePropertyItemLocation(props.noteId, props.propertyKey!, {
        value: props.propertyValue,
        occurrence: 1,
      })
)
</script>
