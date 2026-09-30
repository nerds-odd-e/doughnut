<template>
  <dl class="flex flex-col gap-1 text-sm">
    <div
      v-for="row in propertyRows"
      :key="row.key"
      class="grid grid-cols-[minmax(6rem,40%)_minmax(0,1fr)] gap-x-4 gap-y-1"
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
      <dt class="break-words font-medium text-base-content/80">{{ row.key }}</dt>
      <dd class="m-0 break-words">
        <RichFrontmatterReadOnlyPropertyValue
          :row="row"
          :wiki-links="wikiLinks"
          :last-saved-markdown="lastSavedMarkdown"
        />
      </dd>
    </div>
  </dl>
</template>

<script setup lang="ts">
import RichFrontmatterReadOnlyPropertyValue from "@/components/form/RichFrontmatterReadOnlyPropertyValue.vue"
import { useFocusedNoteProperty } from "@/composables/useFocusedNoteProperty"
import type { WikiLink } from "@generated/donut-backend-api"
import type { PropertyRow } from "@/utils/noteContentFrontmatter"

defineProps<{
  propertyRows: PropertyRow[]
  wikiLinks: WikiLink[]
  lastSavedMarkdown?: string
}>()

const { isFocusedProperty, setPropertyRowRef } = useFocusedNoteProperty()
</script>
