<template>
  <router-link
    v-if="resolved?.noteId !== undefined"
    :to="noteShowLocation(resolved.noteId)"
    :class="DONUT_WIKI_LINK_CLASS"
    v-bind="resolved.linkAttrs"
    >{{ resolved.display }}</router-link
  >
  <a
    v-else-if="resolved"
    href="#"
    :class="DEAD_WIKI_LINK_CLASS"
    v-bind="resolved.linkAttrs"
    @click.prevent="
      emit('deadWikiLinkClick', {
        targetToken: resolved.target,
        displayText: resolved.display,
      })
    "
    >{{ resolved.display }}</a
  >
  <template v-else>{{ token }}</template>
</template>

<script setup lang="ts">
import { computed, type PropType } from "vue"
import type { WikiTitle } from "@generated/donut-backend-api"
import { noteShowLocation } from "@/routes/noteShowLocation"
import {
  noteIdForAuthoredToken,
  parseWholeWikiLinkItem,
} from "@/utils/authoredLinkMarkup"
import {
  DEAD_WIKI_LINK_CLASS,
  DONUT_WIKI_LINK_CLASS,
} from "@/utils/wikiLinkDomMarkers"
import {
  wikiTitleNoteIdLookup,
  type DeadWikiLinkPayload,
} from "@/utils/wikiLinkMarkup"

const props = defineProps({
  token: { type: String, required: true },
  wikiTitles: {
    type: Array as PropType<WikiTitle[]>,
    default: () => [],
  },
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
    target: parsed.target,
    display: parsed.display,
    noteId,
    linkAttrs,
  }
})
</script>
