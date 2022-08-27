<template>
  <NoteShell
    v-if="note"
    v-bind="{ id: note.id, updatedAt: note.textContent?.updatedAt }"
  >
    <NoteFrameOfLinks
      v-if="links && links.links"
      v-bind="{ links, historyWriter }"
      @note-realm-updated="$emit('noteRealmUpdated', $event)"
    >
      <NoteContent
        v-bind="{ note, historyWriter }"
        @note-realm-updated="$emit('noteRealmUpdated', $event)"
      />
    </NoteFrameOfLinks>
    <template #footer>
      <slot name="footer" />
    </template>
  </NoteShell>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteFrameOfLinks from "../links/NoteFrameOfLinks.vue";
import NoteShell from "./NoteShell.vue";
import NoteContent from "./NoteContent.vue";
import { HistoryWriter } from "../../store/history";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
    links: {
      type: Object as PropType<Generated.LinksOfANote>,
    },
    historyWriter: {
      type: Object as PropType<HistoryWriter>,
    },
  },
  emits: ["noteRealmUpdated"],
  components: {
    NoteFrameOfLinks,
    NoteShell,
    NoteContent,
  },
});
</script>

<style scoped>
.note-title {
  margin-top: 0px;
  padding-top: 10px;
  color: black;
}

.outdated-label {
  display: inline-block;
  height: 100%;
  vertical-align: middle;
  margin-left: 20px;
  padding-bottom: 10px;
  color: red;
}
</style>
