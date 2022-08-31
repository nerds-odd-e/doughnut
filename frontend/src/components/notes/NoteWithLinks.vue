<template>
  <NoteShell
    v-if="note"
    v-bind="{ id: note.id, updatedAt: note.textContent?.updatedAt }"
  >
    <NoteFrameOfLinks
      v-if="links && links.links"
      v-bind="{ links, storageAccessor }"
      @note-realm-updated="$emit('noteRealmUpdated', $event)"
    >
      <NoteContent v-bind="{ note, storageAccessor }" />
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
import { StorageAccessor } from "../../store/createNoteStorage";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
    links: {
      type: Object as PropType<Generated.LinksOfANote>,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
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
