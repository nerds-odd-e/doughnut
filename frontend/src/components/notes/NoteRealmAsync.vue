<template>
  <LoadingPage v-bind="{ loading, contentExists: true }">
    <div class="inner-box" :key="noteId">
      <div class="header">
        <NoteToolbar
          v-if="selectedNote && selectedNotePosition"
          v-bind="{ selectedNote, selectedNotePosition, viewType }"
          @note-deleted="onNoteDeleted"
          @note-realm-updated="noteRealmUpdated($event)"
          @new-note-added="newNoteAdded($event)"
        />
      </div>
      <div class="content" v-if="noteRealm && noteRealmCache">
        <NoteMindmapView
          v-if="viewType === 'mindmap'"
          v-bind="{ noteId, noteRealms: noteRealmCache, expandChildren }"
          :highlight-note-id="selectedNoteId"
          @selectNote="highlight($event)"
          @note-realm-updated="noteRealmUpdated($event)"
        />
        <div class="container" v-if="viewType === 'article'">
          <NoteArticleView
            v-bind="{ noteId, noteRealms: noteRealmCache, expandChildren }"
            @note-realm-updated="noteRealmUpdated($event)"
          />
        </div>
        <NoteCardsView
          v-if="!viewType || viewType === 'cards'"
          v-bind="{ noteRealm, expandChildren, comments }"
          @note-realm-updated="noteRealmUpdated($event)"
        />
      </div>
    </div>
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteToolbar from "../toolbars/NoteToolbar.vue";
import NoteMindmapView from "./views/NoteMindmapView.vue";
import NoteCardsView from "./views/NoteCardsView.vue";
import NoteArticleView from "./views/NoteArticleView.vue";
import { ViewTypeName } from "../../models/viewTypes";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import LoadingPage from "../../pages/commons/LoadingPage.vue";
import NoteRealmCache from "../../store/NoteRealmCache";

export default defineComponent({
  setup() {
    return useStoredLoadingApi({ initalLoading: true });
  },
  props: {
    noteId: { type: Number, required: true },
    viewType: {
      type: String as PropType<ViewTypeName>,
      default: () => "cards",
    },
    expandChildren: { type: Boolean, required: true },
  },
  components: {
    LoadingPage,
    NoteToolbar,
    NoteMindmapView,
    NoteCardsView,
    NoteArticleView,
  },
  data() {
    return {
      comments: [] as Generated.Comment[],
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
    selectedNote() {
      return this.noteRealm?.note;
    },
    user() {
      return this.piniaStore.currentUser;
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
    highlight(id: Doughnut.ID) {
      this.selectedNoteId = id;
    },
    async fetchData() {
      if (this.viewType === "mindmap" || this.viewType === "article") {
        this.noteRealmCache = new NoteRealmCache(
          await this.storedApi.getNoteWithDescendents(this.noteId)
        );
      } else {
        this.noteRealmCache = new NoteRealmCache(
          await this.storedApi.getNoteRealmWithPosition(this.noteId)
        );
      }

      if (!this.user) return;
      this.comments = await this.api.comments.getNoteComments(this.noteId);
    },
  },
  watch: {
    noteId() {
      this.fetchData();
    },
    viewType() {
      this.fetchData();
    },
  },
  mounted() {
    this.highlight(this.noteId);
    this.fetchData();
  },
});
</script>
