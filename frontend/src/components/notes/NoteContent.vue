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

    <template #note-content-other>
      <ShowPicture
        v-if="note.noteAccessory?.pictureWithMask"
        class="text-center"
        v-bind="note.noteAccessory.pictureWithMask"
        :opacity="0.2"
      />
      <div v-if="!!note?.noteAccessory?.url">
        <label id="note-url" v-text="'Url:'" />
        <a
          aria-labelledby="note-url"
          :href="note.noteAccessory.url"
          target="_blank"
          >{{ note.noteAccessory.url }}</a
        >
      </div>
    </template>
  </NoteTextContent>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note } from "@/generated/backend";
import ShowPicture from "./ShowPicture.vue";
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
    ShowPicture,
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
