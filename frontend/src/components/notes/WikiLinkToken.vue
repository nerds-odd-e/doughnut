<template>
  <router-link
    v-if="resolved?.noteId !== undefined"
    :to="locationForResolvedWikiTarget(resolved.noteId, resolved.portablePath)"
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
import type { WikiLink } from "@generated/donut-backend-api"
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
  WIKI_LINK_DISPLAY_TEXT_ATTR,
  WIKI_LINK_PORTABLE_PATH_ATTR,
  WIKI_LINK_RESOLUTION_ATTR,
} from "@/utils/wikiLinkDomMarkers"
import {
  wikiLinkAmbiguousResolution,
  wikiLinkNoteIdLookup,
  type DeadWikiLinkPayload,
} from "@/utils/wikiLinkMarkup"
import { locationForResolvedWikiTarget } from "@/utils/wikiLinkResolvedLocation"

const props = defineProps({
  token: { type: String, required: true },
  wikiLinks: {
    type: Array as PropType<WikiLink[]>,
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
  const map = wikiLinkNoteIdLookup(props.wikiLinks)
  const noteId = noteIdForAuthoredToken(parsed.inner, map)
  const linkAttrs: Record<string, string> = {
    [WIKI_LINK_PORTABLE_PATH_ATTR]: parsed.target,
  }
  if (parsed.display !== parsed.target) {
    linkAttrs[WIKI_LINK_DISPLAY_TEXT_ATTR] = parsed.display
  }
  const resolution = wikiLinkAmbiguousResolution(
    props.wikiLinks,
    parsed.target,
    parsed.inner
  )
  if (resolution !== undefined) {
    linkAttrs[WIKI_LINK_RESOLUTION_ATTR] = resolution
  }
  return {
    inner: parsed.inner,
    portablePath: parsed.target,
    display: parsed.display,
    noteId,
    linkAttrs,
    resolution,
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
  const clicked = resolved.value
  if (!clicked || unresolvedClass.value !== DEAD_WIKI_LINK_CLASS) return
  emit("deadWikiLinkClick", {
    portablePath: clicked.portablePath,
    displayText: clicked.display,
    ...(clicked.resolution === undefined
      ? {}
      : { resolution: clicked.resolution }),
  })
}
</script>
