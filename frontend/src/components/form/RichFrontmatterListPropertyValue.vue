<template>
  <span
    v-if="!showUrlLinks && !showWikiLinks"
    class="block min-w-0 truncate text-sm text-base-content/90"
    :title="title"
    data-testid="rich-note-property-row-list-value"
    >{{ compactDisplayForPropertyValue(value) }}</span
  >
  <span
    v-else-if="showWikiLinks"
    class="rich-content-links inline-flex min-w-0 max-w-full flex-wrap items-center text-sm text-base-content/90"
    :title="title"
    data-testid="rich-note-property-row-list-value"
  >
    <template v-for="(item, index) in value.items" :key="index">
      <span v-if="index > 0" aria-hidden="true">, </span>
      <WikiLinkToken
        :token="item"
        :wiki-titles="wikiTitles ?? []"
        @dead-wiki-link-click="emit('deadWikiLinkClick', $event)"
      />
    </template>
  </span>
  <span
    v-else
    class="inline-flex min-w-0 max-w-full flex-wrap items-center text-sm text-base-content/90"
    :title="title"
    data-testid="rich-note-property-row-list-value"
  >
    <template v-for="(item, index) in value.items" :key="index">
      <span v-if="index > 0" aria-hidden="true">, </span>
      <span class="inline-flex min-w-0 max-w-full items-center gap-1">
        <span class="truncate">{{ item }}</span>
        <RichFrontmatterPropertyExternalLink
          v-if="item.trim()"
          kind="url"
          :value="item"
          :compact="compact"
        />
      </span>
    </template>
  </span>
</template>

<script setup lang="ts">
import { computed } from "vue"
import RichFrontmatterPropertyExternalLink from "@/components/form/RichFrontmatterPropertyExternalLink.vue"
import WikiLinkToken from "@/components/notes/WikiLinkToken.vue"
import type { WikiTitle } from "@generated/doughnut-backend-api"
import { isOverlapsPropertyKey } from "@/utils/authoredOverlapsValidation"
import { isUrlPropertyKey } from "@/utils/noteContentPropertyKeys"
import {
  compactDisplayForPropertyValue,
  type PropertyValue,
} from "@/utils/noteProperties"
import type { DeadWikiLinkPayload } from "@/utils/wikiPropertyValueField"

const props = defineProps<{
  value: Extract<PropertyValue, { kind: "list" }>
  propertyKey?: string
  wikiTitles?: WikiTitle[]
  compact?: boolean
}>()

const emit = defineEmits<{
  deadWikiLinkClick: [payload: DeadWikiLinkPayload]
}>()

const showUrlLinks = computed(
  () => props.propertyKey !== undefined && isUrlPropertyKey(props.propertyKey)
)

const showWikiLinks = computed(
  () =>
    props.propertyKey !== undefined && isOverlapsPropertyKey(props.propertyKey)
)

const title = computed(() =>
  props.value.items.length === 0 ? "[]" : props.value.items.join("\n")
)
</script>
