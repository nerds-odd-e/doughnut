<template>
  <dl
    class="daisy-grid daisy-grid-cols-[auto_minmax(0,1fr)] daisy-gap-x-4 daisy-gap-y-1 daisy-text-sm"
  >
    <template v-for="row in propertyRows" :key="row.key">
      <dt class="daisy-font-medium daisy-text-base-content/80">{{ row.key }}</dt>
      <dd class="daisy-m-0">
        <template v-if="isRelationPropertyKey(row.key)">{{
          relationLabelFromKebab(row.value)
        }}</template>
        <span
          v-else-if="isWikidataIdPropertyKey(row.key)"
          class="daisy-inline-flex daisy-min-w-0 daisy-max-w-full daisy-items-center daisy-gap-1"
        >
          <span class="daisy-truncate daisy-font-mono">{{
            row.value.trim() || "—"
          }}</span>
          <RichFrontmatterPropertyExternalLink
            kind="wikidata"
            :value="row.value"
            compact
          />
        </span>
        <span
          v-else-if="isUrlPropertyKey(row.key)"
          class="daisy-inline-flex daisy-min-w-0 daisy-max-w-full daisy-items-center daisy-gap-1"
        >
          <span class="daisy-truncate">{{ row.value }}</span>
          <RichFrontmatterPropertyExternalLink
            kind="url"
            :value="row.value"
            compact
          />
        </span>
        <template v-else>{{ row.value }}</template>
      </dd>
    </template>
  </dl>
</template>

<script setup lang="ts">
import RichFrontmatterPropertyExternalLink from "@/components/form/RichFrontmatterPropertyExternalLink.vue"
import { relationLabelFromKebab } from "@/models/relationTypeOptions"
import {
  isRelationPropertyKey,
  isUrlPropertyKey,
  isWikidataIdPropertyKey,
  type PropertyRow,
} from "@/utils/noteContentFrontmatter"

defineProps<{ propertyRows: PropertyRow[] }>()
</script>
