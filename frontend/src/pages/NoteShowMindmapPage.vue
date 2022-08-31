<template>
  <LoadingPage v-bind="{ loading, contentExists: true }">
    <div class="inner-box" :key="noteId">
      <div class="header">
        <NoteToolbar
          v-if="selectedNoteId && selectedNotePosition"
          v-bind="{
            selectedNoteId,
            selectedNotePosition,
            viewType: 'mindmap',
            storageAccessor,
          }"
        />
      </div>
      <div class="content" v-if="noteRealm && noteRealmCache">
        <NoteMindmapView
          v-bind="{ noteId, noteRealms: noteRealmCache, storageAccessor }"
          :highlight-note-id="selectedNoteId"
          @select-note="highlight($event)"
        />
      </div>
    </div>
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteToolbar from "../components/toolbars/NoteToolbar.vue";
import NoteMindmapView from "../components/notes/views/NoteMindmapView.vue";
import useLoadingApi from "../managedApi/useLoadingApi";
import LoadingPage from "./commons/LoadingPage.vue";
import NoteRealmCache from "../store/NoteRealmCache";
import { StorageAccessor } from "../store/createNoteStorage";

export default defineComponent({
  setup() {
    return useLoadingApi({ initalLoading: true });
  },
  props: {
    noteId: { type: Number, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    LoadingPage,
    NoteToolbar,
    NoteMindmapView,
  },
  data() {
    return {
      noteRealmCache: undefined as NoteRealmCache | undefined,
      selectedNoteId: undefined as Doughnut.ID | undefined,
    };
  },
  computed: {
    noteRealm() {
      return this.noteRealmCache?.getNoteRealmById(this.noteId);
    },
    selectedNotePosition(): Generated.NotePositionViewedByUser | undefined {
      return this.noteRealmCache?.getNotePosition(this.selectedNoteId);
    },
  },
  methods: {
    onNoteDeleted(deletedNoteId: Doughnut.ID) {
      this.noteRealmCache?.deleteNoteAndDescendents(deletedNoteId);
    },
    noteRealmUpdated(updatedNoteRealm?: Generated.NoteRealm) {
      if (!updatedNoteRealm) {
        this.fetchData();
        return;
      }
      this.noteRealmCache?.updateNoteRealm(updatedNoteRealm);
    },
    highlight(id: Doughnut.ID) {
      this.selectedNoteId = id;
    },
    async fetchData() {
      this.noteRealmCache = new NoteRealmCache(
        await this.api.noteMethods.getNoteWithDescendents(this.noteId)
      );
    },
  },
  watch: {
    "storageAccessor.updatedAt": function updateAt() {
      this.noteRealmUpdated(this.storageAccessor.updatedNoteRealm);
    },
    noteId() {
      this.fetchData();
    },
  },
  mounted() {
    this.highlight(this.noteId);
    this.fetchData();
  },
});
</script>
