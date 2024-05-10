<template>
  <div class="alert alert-warning" v-if="note.deletedAt">
    This note has been deleted
  </div>
  <NoteTextContent :note="note" :storage-accessor="storageAccessor">
    <template #topic-additional>
      <div class="header-options">
        <NoteWikidataAssociation
          v-if="note.wikidataId"
          :wikidata-id="note.wikidataId"
        />
      </div>
    </template>
  </NoteTextContent>
  <NoteAccessoryAsync :note-id="note.id" :note-accessory="note.noteAccessory" />
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note } from "@/generated/backend";
import NoteAccessoryAsync from "./NoteAccessoryAsync.vue";
import NoteWikidataAssociation from "./NoteWikidataAssociation.vue";
import type { StorageAccessor } from "../../store/createNoteStorage";
import NoteTextContent from "./NoteTextContent.vue";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Note>, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    NoteAccessoryAsync,
    NoteWikidataAssociation,
    NoteTextContent,
  },
});
</script>

<style lang="sass" scoped>
.header-options
  display: flex
  align-items: center
  margin: 0 10px
</style>
