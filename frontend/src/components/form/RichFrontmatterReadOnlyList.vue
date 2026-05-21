<template>
  <dl
    class="grid grid-cols-[auto_minmax(0,1fr)] gap-x-4 gap-y-1 text-sm"
  >
    <template v-for="row in propertyRows" :key="row.key">
      <dt class="font-medium text-base-content/80">{{ row.key }}</dt>
      <dd class="m-0">
        <template v-if="isRelationPropertyKey(row.key)">{{
          relationLabelFromKebab(row.value)
        }}</template>
        <span
          v-else-if="isWikidataIdPropertyKey(row.key)"
          class="inline-flex min-w-0 max-w-full items-center gap-1"
        >
          <span class="truncate font-mono">{{
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
          class="inline-flex min-w-0 max-w-full items-center gap-1"
        >
          <span class="truncate">{{ row.value }}</span>
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
