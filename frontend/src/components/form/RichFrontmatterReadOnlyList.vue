<template>
  <dl
    class="grid grid-cols-[auto_minmax(0,1fr)] gap-x-4 gap-y-1 text-sm"
  >
    <template v-for="row in propertyRows" :key="row.key">
      <dt class="font-medium text-base-content/80">{{ row.key }}</dt>
      <dd class="m-0">
        <RichFrontmatterListPropertyValue
          v-if="isListPropertyValue(row.value)"
          :value="row.value"
        />
        <template v-else-if="isRelationPropertyKey(row.key)">{{
          relationLabelFromKebab(row.value.value)
        }}</template>
        <span
          v-else-if="isWikidataIdPropertyKey(row.key)"
          class="inline-flex min-w-0 max-w-full items-center gap-1"
        >
          <span class="truncate font-mono">{{
            row.value.value.trim() || "—"
          }}</span>
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
          <span class="truncate">{{ row.value.value }}</span>
          <RichFrontmatterPropertyExternalLink
            kind="url"
            :value="row.value.value"
            compact
          />
        </span>
        <template v-else>{{ row.value.value }}</template>
      </dd>
    </template>
  </dl>
</template>

<script setup lang="ts">
import RichFrontmatterListPropertyValue from "@/components/form/RichFrontmatterListPropertyValue.vue"
import RichFrontmatterPropertyExternalLink from "@/components/form/RichFrontmatterPropertyExternalLink.vue"
import { relationLabelFromKebab } from "@/models/relationTypeOptions"
import {
  isRelationPropertyKey,
  isUrlPropertyKey,
  isWikidataIdPropertyKey,
  type PropertyRow,
} from "@/utils/noteContentFrontmatter"
import { isListPropertyValue } from "@/utils/noteProperties"

defineProps<{ propertyRows: PropertyRow[] }>()
</script>
