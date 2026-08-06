<template>
  <router-link
    v-if="resolved?.noteId !== undefined"
    :to="noteShowLocation(resolved.noteId)"
    class="doughnut-link"
    v-bind="resolved.linkAttrs"
    >{{ resolved.display }}</router-link
  >
  <a
    v-else-if="resolved"
    href="#"
    class="dead-link"
    v-bind="resolved.linkAttrs"
    @click.prevent="
      emit('deadLinkClick', {
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
import type { WikiTitle } from "@generated/doughnut-backend-api"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { parseWholeWikiLinkItem } from "@/utils/wholeWikiLinkItem"
import {
  wikiTitleNoteIdLookup,
  type DeadLinkPayload,
} from "@/utils/wikiPropertyValueField"

const props = defineProps({
  token: { type: String, required: true },
  wikiTitles: {
    type: Array as PropType<WikiTitle[]>,
    default: () => [],
  },
})

const emit = defineEmits<{
  deadLinkClick: [payload: DeadLinkPayload]
}>()

const resolved = computed(() => {
  const parsed = parseWholeWikiLinkItem(props.token.trim())
  if (!parsed) return undefined
  const map = wikiTitleNoteIdLookup(props.wikiTitles)
  const noteId = map.get(parsed.inner.trim()) ?? map.get(parsed.target.trim())
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
