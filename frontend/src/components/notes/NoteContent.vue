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
  <div>
    <a>My attach file:</a>
  </div>
  <button
    @click="download()"
    class="btn btn-sm btn-secondary"
    title="Download audio file"
  >
    <i class="fas fa-download"></i> Download audio file
  </button>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note } from "@/generated/backend";
import ShowPicture from "./ShowPicture.vue";
import NoteWikidataAssociation from "./NoteWikidataAssociation.vue";
import type { StorageAccessor } from "../../store/createNoteStorage";
import NoteTextContent from "./NoteTextContent.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return { ...useLoadingApi() };
  },
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
  methods: {
    async download() {
      try {
        await this.managedApi.restNoteController.download(1); // Call server endpoint

        // // Assuming server response is an object with 'fileName' and 'data' properties
        // const { fileName, data } = response;

        // // Trigger file download in browser
        // const blob = new Blob([data], { type: 'application/octet-stream' });
        // const url = URL.createObjectURL(blob);
        // const link = document.createElement('a');
        // link.href = url;
        // link.setAttribute('download', fileName); // Optional: set filename
        // document.body.appendChild(link);
        // link.click();
        // document.body.removeChild(link);
      } catch (error) {
        // console.error(`Error downloading file:`, error);
      }
    },
  },
});
</script>

<style lang="sass" scoped>
.header-options
  display: flex
  align-items: center
  margin: 0 10px
</style>
