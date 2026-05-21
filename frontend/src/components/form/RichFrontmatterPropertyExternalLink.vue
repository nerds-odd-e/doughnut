<script setup lang="ts">
import { ExternalLink } from "@lucide/vue"
import { computed } from "vue"
import { openRichPropertyUrlInNewWindow } from "@/utils/openRichPropertyUrlInNewWindow"
import { openWikidataEntityBrowseUrlInNonBlockingPopup } from "@/utils/wikidataEntityBrowseUrl"

const props = withDefaults(
  defineProps<{
    kind: "wikidata" | "url"
    value: string
    /** Tighter control for read-only definition list rows */
    compact?: boolean
  }>(),
  { compact: false }
)

const trimmed = computed(() => props.value.trim())

const title = computed(() =>
  props.kind === "wikidata" ? "Open in browser" : "Open URL in new tab"
)

const ariaLabel = computed(() =>
  props.kind === "wikidata"
    ? `Open Wikidata entity ${trimmed.value} in browser`
    : "Open URL in new tab"
)

const btnClass = computed(() =>
  props.compact
    ? "daisy-btn daisy-btn-ghost daisy-btn-xs square shrink-0"
    : "daisy-btn daisy-btn-ghost daisy-btn-sm square shrink-0"
)

const iconClass = computed(() => (props.compact ? "h-3.5 w-3.5" : "h-4 w-4"))

function onClick() {
  if (!trimmed.value) return
  if (props.kind === "wikidata") {
    openWikidataEntityBrowseUrlInNonBlockingPopup(props.value).catch(
      () => undefined
    )
  } else {
    openRichPropertyUrlInNewWindow(props.value)
  }
}
</script>

<template>
  <button
    v-if="trimmed"
    type="button"
    :class="btnClass"
    :title="title"
    :aria-label="ariaLabel"
    data-testid="rich-note-property-external-link"
    @click="onClick"
  >
    <ExternalLink :class="iconClass" aria-hidden="true" />
  </button>
</template>
