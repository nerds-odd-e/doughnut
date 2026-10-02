<template>
  <span
    v-if="!showUrlLinks && !showWikiLinks"
    class="block min-w-0 truncate text-sm text-base-content/90"
    :title="title"
    data-testid="rich-note-property-row-list-value"
    >{{ compactDisplayForPropertyValue(value) }}</span
  >
  <span
    v-else
    class="rich-content-wiki-links inline-flex min-w-0 max-w-full flex-wrap items-center text-sm text-base-content/90"
    :title="title"
    data-testid="rich-note-property-row-list-value"
  >
    <template v-for="(item, index) in value.items" :key="index">
      <span v-if="index > 0" aria-hidden="true">, </span>
      <WikiLinkToken
        v-if="parseWholeWikiLinkItem(item.trim())"
        :token="item"
        :wiki-links="wikiLinks ?? []"
        :last-saved-markdown="lastSavedMarkdown"
        @dead-wiki-link-click="emit('deadWikiLinkClick', $event)"
      />
      <span v-else class="inline-flex min-w-0 max-w-full items-center gap-1">
        <span class="truncate">{{ item }}</span>
        <RichFrontmatterPropertyExternalLink
          v-if="showUrlLinks && item.trim()"
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
import type { WikiLink } from "@generated/donut-backend-api"
import { parseWholeWikiLinkItem } from "@/utils/authoredLinkMarkup"
import { isUrlPropertyKey } from "@/utils/noteContentPropertyKeys"
import {
  compactDisplayForPropertyValue,
  type PropertyValue,
} from "@/utils/noteProperties"
import type { DeadWikiLinkPayload } from "@/utils/wikiLinkMarkup"

const props = defineProps<{
  value: Extract<PropertyValue, { kind: "list" }>
  propertyKey?: string
  wikiLinks?: WikiLink[]
  lastSavedMarkdown?: string
  compact?: boolean
}>()

const emit = defineEmits<{
  deadWikiLinkClick: [payload: DeadWikiLinkPayload]
}>()

const showUrlLinks = computed(
  () => props.propertyKey !== undefined && isUrlPropertyKey(props.propertyKey)
)

const showWikiLinks = computed(() =>
  props.value.items.some((item) => parseWholeWikiLinkItem(item.trim()))
)

const title = computed(() =>
  props.value.items.length === 0 ? "[]" : props.value.items.join("\n")
)
</script>
