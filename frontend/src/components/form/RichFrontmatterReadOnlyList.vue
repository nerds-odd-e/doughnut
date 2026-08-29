<template>
  <dl class="flex flex-col gap-1 text-sm">
    <div
      v-for="row in propertyRows"
      :key="row.key"
      class="grid grid-cols-[auto_minmax(0,1fr)] gap-x-4 gap-y-1"
      data-testid="rich-note-property-row"
      :data-property-key="row.key"
      :data-property-focused="isFocusedProperty(row.key) ? 'true' : undefined"
      :class="{
        'rounded bg-primary/10 ring-1 ring-primary/30': isFocusedProperty(
          row.key
        ),
      }"
      :ref="(el) => setPropertyRowRef(row.key, el)"
    >
      <dt class="font-medium text-base-content/80">{{ row.key }}</dt>
      <dd class="m-0">
        <RichFrontmatterListPropertyValue
          v-if="isListPropertyValue(row.value)"
          :value="row.value"
          :property-key="row.key"
          :wiki-titles="wikiTitles"
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
    </div>
  </dl>
</template>

<script setup lang="ts">
import RichFrontmatterListPropertyValue from "@/components/form/RichFrontmatterListPropertyValue.vue"
import RichFrontmatterPropertyExternalLink from "@/components/form/RichFrontmatterPropertyExternalLink.vue"
import { useFocusedNoteProperty } from "@/composables/useFocusedNoteProperty"
import type { WikiTitle } from "@generated/donut-backend-api"
import { relationLabelFromKebab } from "@/models/relationTypeOptions"
import {
  isRelationPropertyKey,
  isUrlPropertyKey,
  isWikidataIdPropertyKey,
  type PropertyRow,
} from "@/utils/noteContentFrontmatter"
import { isListPropertyValue } from "@/utils/noteProperties"

defineProps<{
  propertyRows: PropertyRow[]
  wikiTitles: WikiTitle[]
  lastSavedMarkdown?: string
}>()

const { isFocusedProperty, setPropertyRowRef } = useFocusedNoteProperty()
</script>
