<template>
  <NoteFrameOfLinks v-if="links" v-bind="{ links, storageAccessor }">
    <div class="alert alert-warning" v-if="note.deletedAt">
      This note has been deleted
    </div>
    <NoteEditableTopic
      :note-id="note.id"
      :note-topic-constructor="note.topicConstructor"
      :note-topic="note.topic"
      :storage-accessor="storageAccessor"
    />
    <div role="details" class="note-details">
      <NoteEditableDetails
        :note-id="note.id"
        :note-details="note.details"
        :storage-accessor="storageAccessor"
      />
    </div>
  </NoteFrameOfLinks>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note } from "@/generated/backend";
import NoteFrameOfLinks from "./NoteFrameOfLinks.vue";
import { StorageAccessor } from "../../../store/createNoteStorage";
import LinksMap from "../../../models/LinksMap";
import NoteEditableTopic from "./NoteEditableTopic.vue";
import NoteEditableDetails from "./NoteEditableDetails.vue";

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
    NoteEditableTopic,
    NoteEditableDetails,
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
