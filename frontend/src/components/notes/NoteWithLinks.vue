<template>
  <NoteShell v-if="note" v-bind="{ id: note.id, updatedAt: note.updatedAt }">
    <NoteFrameOfLinks v-if="links" v-bind="{ links, storageAccessor }">
      <NoteContent v-bind="{ note, storageAccessor }" />
    </NoteFrameOfLinks>
    <template #footer>
      <slot name="footer" />
    </template>
  </NoteShell>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note } from "@/generated/backend";
import NoteFrameOfLinks from "../links/NoteFrameOfLinks.vue";
import NoteShell from "./NoteShell.vue";
import NoteContent from "./NoteContent.vue";
import { StorageAccessor } from "../../store/createNoteStorage";
import LinksMap from "../../models/LinksMap";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Note>, required: true },
    links: {
      type: Object as PropType<LinksMap>,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    NoteFrameOfLinks,
    NoteShell,
    NoteContent,
  },
});
</script>

<style scoped>
.note-topic {
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
