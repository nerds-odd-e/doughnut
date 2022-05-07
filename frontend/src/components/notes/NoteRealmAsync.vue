<template>
  <LoadingPage v-bind="{ loading, contentExists: true }">
    <div class="inner-box" :key="noteId">
      <div class="header">
        <NoteToolbar
          v-bind="{ selectedNote, selectedNotePosition, viewType }"
        />
      </div>
      <div class="content" v-if="noteRealm">
        <NoteMindmapView
          v-if="viewType === 'mindmap'"
          v-bind="{ noteId, noteRealms, expandChildren }"
          :highlight-note-id="selectedNoteId"
          @selectNote="highlight($event)"
          @note-realm-updated="noteRealmUpdated($event)"
        />
        <div class="container" v-if="viewType === 'article'">
          <NoteArticleView
            v-bind="{ noteId, noteRealms, expandChildren }"
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
import { defineComponent } from "vue";
import NoteToolbar from "../toolbars/NoteToolbar.vue";
import NoteMindmapView from "./views/NoteMindmapView.vue";
import NoteCardsView from "./views/NoteCardsView.vue";
import NoteArticleView from "./views/NoteArticleView.vue";
import { ViewType, viewType } from "../../models/viewTypes";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import LoadingPage from "../../pages/commons/LoadingPage.vue";
import NoteRealmCache from "../../store/NoteRealmCache";

export default defineComponent({
  setup() {
    return useStoredLoadingApi({ initalLoading: true });
  },
  props: {
    noteId: { type: Number, required: true },
    viewType: String,
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
      noteRealms: null as NoteRealmCache | undefined,
      selectedNoteId: undefined as Doughnut.ID | undefined,
    };
  },
  computed: {
    noteRealm() {
      return this.noteRealms?.getNoteRealmById(this.noteId);
    },
    viewTypeObj(): ViewType {
      return viewType(this.viewType);
    },
    selectedNotePosition(): Generated.NotePositionViewedByUser | undefined {
      if (!this.selectedNoteId) return;
      return this.piniaStore.getNotePosition(this.selectedNoteId);
    },
    selectedNote() {
      return this.noteRealm?.note;
    },
    user() {
      return this.piniaStore.currentUser;
    },
  },
  methods: {
    noteRealmUpdated(updatedNoteRealm: Generated.NoteRealm) {},
    highlight(id: Doughnut.ID) {
      this.selectedNoteId = id;
    },
    async fetchComments() {
      if (!this.user) return;
      this.comments = await this.api.comments.getNoteComments(this.noteId);
    },
    async fetchData() {
      const storedApiCall = this.viewTypeObj.fetchAll
        ? this.storedApi.getNoteWithDescendents
        : this.storedApi.getNoteAndItsChildren;

      this.noteRealms = new NoteRealmCache(await storedApiCall(this.noteId));
      await this.fetchComments();
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
