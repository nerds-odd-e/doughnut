<template>
  <router-link
    v-if="resolved?.noteId !== undefined"
    :to="locationForResolvedWikiTarget(resolved.noteId, resolved.target)"
    :class="DONUT_WIKI_LINK_CLASS"
    v-bind="resolved.linkAttrs"
    >{{ resolved.display }}</router-link
  >
  <a
    v-else-if="resolved"
    href="#"
    :class="unresolvedClass"
    v-bind="resolved.linkAttrs"
    @click.prevent="onUnresolvedClick"
    >{{ resolved.display }}</a
  >
  <template v-else>{{ token }}</template>
</template>

<script setup lang="ts">
import { computed, type PropType } from "vue"
import type { WikiTitle } from "@generated/donut-backend-api"
import {
  noteIdForAuthoredToken,
  parseWholeWikiLinkItem,
} from "@/utils/authoredLinkMarkup"
import {
  lastSavedAuthoredTokens,
  unresolvedWikiClass,
} from "@/utils/unresolvedWikiLinkStyle"
import {
  DEAD_WIKI_LINK_CLASS,
  DONUT_WIKI_LINK_CLASS,
} from "@/utils/wikiLinkDomMarkers"
import {
  wikiTitleNoteIdLookup,
  type DeadWikiLinkPayload,
} from "@/utils/wikiLinkMarkup"
import { locationForResolvedWikiTarget } from "@/utils/wikiLinkResolvedLocation"

const props = defineProps({
  token: { type: String, required: true },
  wikiTitles: {
    type: Array as PropType<WikiTitle[]>,
    default: () => [],
  },
  lastSavedMarkdown: { type: String, default: undefined },
})

const emit = defineEmits<{
  deadWikiLinkClick: [payload: DeadWikiLinkPayload]
}>()

const resolved = computed(() => {
  const parsed = parseWholeWikiLinkItem(props.token.trim())
  if (!parsed) return undefined
  const map = wikiTitleNoteIdLookup(props.wikiTitles)
  const noteId = noteIdForAuthoredToken(parsed.inner, map)
  const linkAttrs: Record<string, string> = {
    "data-wiki-title": parsed.target,
  }
  if (parsed.display !== parsed.target) {
    linkAttrs["data-wiki-display"] = parsed.display
  }
  return {
    inner: parsed.inner,
    target: parsed.target,
    display: parsed.display,
    noteId,
    linkAttrs,
  }
})

const unresolvedClass = computed(() => {
  if (!resolved.value) return DEAD_WIKI_LINK_CLASS
  return unresolvedWikiClass(
    resolved.value.inner,
    lastSavedAuthoredTokens(props.lastSavedMarkdown)
  )
})

function onUnresolvedClick() {
  if (!resolved.value || unresolvedClass.value !== DEAD_WIKI_LINK_CLASS) return
  emit("deadWikiLinkClick", {
    targetToken: resolved.value.target,
    displayText: resolved.value.display,
  })
}
</script>
