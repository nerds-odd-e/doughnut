<template>
  <LoadingPage v-bind="{ loading, contentExists: true }">
    <div class="inner-box" :key="noteId">
      <div class="header">
        <NoteToolbar
          v-if="selectedNote && notePosition"
          v-bind="{
            selectedNote,
            selectedNotePosition: notePosition,
            viewType: 'article',
          }"
          @note-deleted="onNoteDeleted"
          @note-realm-updated="noteRealmUpdated($event)"
          @new-note-added="newNoteAdded($event)"
        />
      </div>
      <div class="content" v-if="noteRealm && noteRealmCache">
        <div class="container">
          <NoteArticleView
            v-bind="{ noteId, noteRealms: noteRealmCache }"
            @note-realm-updated="noteRealmUpdated($event)"
          />
        </div>
      </div>
    </div>
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import NoteToolbar from "../components/toolbars/NoteToolbar.vue";
import NoteArticleView from "../components/notes/views/NoteArticleView.vue";
import useStoredLoadingApi from "../managedApi/useStoredLoadingApi";
import LoadingPage from "./commons/LoadingPage.vue";
import NoteRealmCache from "../store/NoteRealmCache";

export default defineComponent({
  setup() {
    return useStoredLoadingApi({ initalLoading: true });
  },
  props: {
    noteId: { type: Number, required: true },
  },
  components: {
    LoadingPage,
    NoteToolbar,
    NoteArticleView,
  },
  data() {
    return {
      noteRealmCache: undefined as NoteRealmCache | undefined,
      notePosition: undefined as Generated.NotePositionViewedByUser | undefined,
    };
  },
  computed: {
    noteRealm() {
      return this.noteRealmCache?.getNoteRealmById(this.noteId);
    },
    selectedNote() {
      return this.noteRealm?.note;
    },
  },
  methods: {
    onNoteDeleted(deletedNoteId: Doughnut.ID) {
      this.noteRealmCache?.deleteNoteAndDescendents(deletedNoteId);
    },
    newNoteAdded(newNote: Generated.NoteRealmWithPosition) {
      this.$router.push({
        name: "noteShow",
        params: { noteId: newNote.notePosition.noteId },
      });
    },
    noteRealmUpdated(updatedNoteRealm?: Generated.NoteRealm) {
      if (!updatedNoteRealm) {
        this.fetchData();
        return;
      }
      this.noteRealmCache?.updateNoteRealm(updatedNoteRealm);
    },
    async fetchData() {
      const noteWithDescendents = await this.storedApi.getNoteWithDescendents(
        this.noteId
      );
      this.notePosition = noteWithDescendents.notePosition;
      this.noteRealmCache = new NoteRealmCache(noteWithDescendents);
    },
  },
  watch: {
    noteId() {
      this.fetchData();
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>
