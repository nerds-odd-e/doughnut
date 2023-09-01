<template>
  <NoteShell
    :class="`inner-box note-card ${size}`"
    v-bind="{ id: note.id, updatedAt: note.updatedAt }"
    role="card"
    :aria-label="note.title"
    :style="`top:${coord.y}px; left:${coord.x}px`"
    @click="$emit('highlight', note.id)"
  >
    <NoteContent v-bind="{ note, size, storageAccessor }" />
  </NoteShell>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteShell from "../NoteShell.vue";
import NoteContent from "../NoteContent.vue";
import MindmapSector from "../../../models/MindmapSector";
import Mindmap from "../../../models/Mindmap";
import { StorageAccessor } from "../../../store/createNoteStorage";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
    mindmapSector: { type: MindmapSector, required: true },
    mindmap: { type: Object as PropType<Mindmap>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  emits: ["highlight"],
  components: {
    NoteShell,
    NoteContent,
  },
  computed: {
    coord() {
      return this.mindmap.coord(this.mindmapSector);
    },
    size() {
      return this.mindmap.size();
    },
  },
});
</script>

<style lang="sass" scoped>
.note-card
  z-index: 2000
  position: absolute
  width: 150px
  height: 50px
  padding: 3px
  background-color: white
  border-width: 3px
  border-style: solid
  border-color: rgb(0,0,0, 0.7)
  border-radius: 10px
  &.medium
    width: 200px
    height: 100px
  &.large
    width: 300px
    height: 200px
.note-card-title
  font-size: 1rem

:deep(.note-description)
  height: 100%
</style>
