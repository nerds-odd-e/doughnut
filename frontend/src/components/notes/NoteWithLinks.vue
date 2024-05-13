<template>
  <ControlCenterForNote v-if="!readonly" v-bind="{ note, storageAccessor }" />
  <NoteFrameOfLinks v-if="links" v-bind="{ links, storageAccessor }">
    <div class="alert alert-warning" v-if="note.deletedAt">
      This note has been deleted
    </div>
    <NoteTextContent :note="note" :storage-accessor="storageAccessor" />
  </NoteFrameOfLinks>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note } from "@/generated/backend";
import NoteFrameOfLinks from "../links/NoteFrameOfLinks.vue";
import NoteTextContent from "./NoteTextContent.vue";
import { StorageAccessor } from "../../store/createNoteStorage";
import LinksMap from "../../models/LinksMap";
import ControlCenterForNote from "../toolbars/ControlCenterForNote.vue";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Note>, required: true },
    links: {
      type: Object as PropType<LinksMap>,
    },
    readonly: { type: Boolean, default: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    NoteFrameOfLinks,
    NoteTextContent,
    ControlCenterForNote,
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
