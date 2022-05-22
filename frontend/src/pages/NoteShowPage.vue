<template>
  <LoadingPage v-bind="{ loading, contentExists: !!noteRealm }">
    <div class="inner-box" v-if="noteRealm" :key="noteId">
      <div class="header">
        <NoteToolbar
          v-if="notePosition"
          v-bind="{
            selectedNote: noteRealm.note,
            selectedNotePosition: notePosition,
            viewType: 'cards',
          }"
          @note-deleted="onNoteDeleted"
          @note-realm-updated="noteRealmUpdated($event)"
          @new-note-added="newNoteAdded($event)"
        />
      </div>
      <div class="content" v-if="noteRealm">
        <NoteCardsView
          v-bind="{ noteRealm, expandChildren, comments }"
          @note-realm-updated="noteRealmUpdated($event)"
        />
      </div>
    </div>
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import NoteToolbar from "../components/toolbars/NoteToolbar.vue";
import NoteCardsView from "../components/notes/views/NoteCardsView.vue";
import useStoredLoadingApi from "../managedApi/useStoredLoadingApi";
import LoadingPage from "./commons/LoadingPage.vue";

export default defineComponent({
  setup() {
    return useStoredLoadingApi({ initalLoading: true });
  },
  props: {
    noteId: { type: Number, required: true },
    expandChildren: { type: Boolean, default: true },
  },
  components: {
    LoadingPage,
    NoteToolbar,
    NoteCardsView,
  },
  data() {
    return {
      comments: [] as Generated.Comment[],
      notePosition: undefined as Generated.NotePositionViewedByUser | undefined,
      noteRealm: undefined as Generated.NoteRealm | undefined,
      selectedNoteId: undefined as Doughnut.ID | undefined,
    };
  },
  computed: {
    user() {
      return this.piniaStore.currentUser;
    },
  },
  methods: {
    onNoteDeleted() {
      this.$router.push({
        name: "noteShow",
        params: { noteId: this.noteRealm?.note.parentId },
      });
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
      this.noteRealm = updatedNoteRealm;
    },
    async fetchData() {
      const noteRealmWithPosition =
        await this.storedApi.getNoteRealmWithPosition(this.noteId);
      this.notePosition = noteRealmWithPosition.notePosition;
      this.noteRealm = noteRealmWithPosition.noteRealm;
      if (!this.user) return;
      this.comments = await this.api.comments.getNoteComments(this.noteId);
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
