<template>
  <RichFrontmatterListPropertyValue
    v-if="isListPropertyValue(row.value)"
    :value="row.value"
    :property-key="row.key"
    :wiki-links="wikiLinks"
    :last-saved-markdown="lastSavedMarkdown"
    compact
  />
  <template v-else-if="isRelationPropertyKey(row.key)">{{
    relationLabelFromKebab(row.value.value)
  }}</template>
  <span
    v-else-if="isWikidataIdPropertyKey(row.key)"
    class="inline-flex min-w-0 max-w-full items-center gap-1"
  >
    <span class="min-w-0 font-mono">{{ row.value.value.trim() || "—" }}</span>
    <RichFrontmatterPropertyExternalLink
      kind="wikidata"
      :value="row.value.value"
      compact
    />
  </span>
  <span
    v-else-if="isUrlPropertyKey(row.key)"
    class="inline-flex min-w-0 max-w-full items-center gap-1"
  >
    <span class="min-w-0">{{ row.value.value }}</span>
    <RichFrontmatterPropertyExternalLink
      kind="url"
      :value="row.value.value"
      compact
    />
  </span>
  <WikiLinkToken
    v-else
    :token="row.value.value"
    :wiki-links="wikiLinks"
    :last-saved-markdown="lastSavedMarkdown"
  />
</template>

<script setup lang="ts">
import RichFrontmatterListPropertyValue from "@/components/form/RichFrontmatterListPropertyValue.vue"
import RichFrontmatterPropertyExternalLink from "@/components/form/RichFrontmatterPropertyExternalLink.vue"
import WikiLinkToken from "@/components/notes/WikiLinkToken.vue"
import type { WikiLink } from "@generated/donut-backend-api"
import { relationLabelFromKebab } from "@/models/relationTypeOptions"
import {
  isRelationPropertyKey,
  isUrlPropertyKey,
  isWikidataIdPropertyKey,
  type PropertyRow,
} from "@/utils/noteContentFrontmatter"
import { isListPropertyValue } from "@/utils/noteProperties"

defineProps<{
  row: PropertyRow
  wikiLinks: WikiLink[]
  lastSavedMarkdown?: string
}>()
</script>
