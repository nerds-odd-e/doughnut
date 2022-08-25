<template>
  <LoadingPage v-bind="{ loading, contentExists: true }">
    <div class="inner-box" :key="noteId">
      <div class="header">
        <NoteToolbar
          v-if="selectedNoteId && selectedNotePosition"
          v-bind="{ selectedNoteId, selectedNotePosition, viewType: 'mindmap' }"
          @note-deleted="onNoteDeleted"
          @note-realm-updated="noteRealmUpdated($event)"
          @new-note-added="newNoteAdded()"
        />
      </div>
      <div class="content" v-if="noteRealm && noteRealmCache">
        <NoteMindmapView
          v-bind="{ noteId, noteRealms: noteRealmCache }"
          :highlight-note-id="selectedNoteId"
          @select-note="highlight($event)"
          @note-realm-updated="noteRealmUpdated($event)"
        />
      </div>
    </div>
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import NoteToolbar from "../components/toolbars/NoteToolbar.vue";
import NoteMindmapView from "../components/notes/views/NoteMindmapView.vue";
import useLoadingApi from "../managedApi/useLoadingApi";
import LoadingPage from "./commons/LoadingPage.vue";
import NoteRealmCache from "../store/NoteRealmCache";

export default defineComponent({
  setup() {
    return useLoadingApi({ initalLoading: true });
  },
  props: {
    noteId: { type: Number, required: true },
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
    newNoteAdded() {
      this.fetchData();
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
