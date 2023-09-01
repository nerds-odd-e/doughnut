<template>
  <div class="alert alert-warning" v-if="note.deletedAt">
    This note has been deleted
  </div>
  <NoteTextContent
    :note-id="note.id"
    :text-content="{
      title: note.title,
      description: note.description,
    }"
    :storage-accessor="storageAccessor"
  >
    <template #title-additional>
      <div class="header-options">
        <NoteWikidataAssociation
          v-if="note.wikidataId"
          :wikidata-id="note.wikidataId"
        />
      </div>
    </template>

    <template #note-content-other>
      <ShowPicture
        v-if="note.pictureWithMask"
        class="text-center"
        v-bind="note.pictureWithMask"
        :opacity="0.2"
      />
      <div v-if="!!note.noteAccessories.url">
        <label id="note-url" v-text="'Url:'" />
        <a
          aria-labelledby="note-url"
          :href="note.noteAccessories.url"
          target="_blank"
          >{{ note.noteAccessories.url }}</a
        >
      </div>
    </template>
  </NoteTextContent>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ShowPicture from "./ShowPicture.vue";
import NoteWikidataAssociation from "./NoteWikidataAssociation.vue";
import type { StorageAccessor } from "../../store/createNoteStorage";
import NoteTextContent from "./NoteTextContent.vue";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
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
