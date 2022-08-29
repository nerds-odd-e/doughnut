<template>
  <LoadingPage v-bind="{ loading, contentExists: true }">
    <div class="inner-box" :key="noteId">
      <div class="content" v-if="noteRealm && noteRealmCache">
        <div class="container">
          <div class="header">
            <ToolbarFrame>
              <ViewTypeButtons v-bind="{ viewType: 'article', noteId }" />
            </ToolbarFrame>
            <Breadcrumb v-bind="notePosition" />
          </div>
          <NoteArticleView
            v-bind="{ noteId, noteRealms: noteRealmCache, historyWriter }"
            @note-realm-updated="noteRealmUpdated($event)"
          />
        </div>
      </div>
    </div>
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteArticleView from "../components/notes/views/NoteArticleView.vue";
import useLoadingApi from "../managedApi/useLoadingApi";
import LoadingPage from "./commons/LoadingPage.vue";
import NoteRealmCache from "../store/NoteRealmCache";
import ToolbarFrame from "../components/toolbars/ToolbarFrame.vue";
import Breadcrumb from "../components/toolbars/Breadcrumb.vue";
import ViewTypeButtons from "../components/toolbars/ViewTypeButtons.vue";
import { HistoryWriter } from "../store/history";

export default defineComponent({
  setup() {
    return useLoadingApi({ initalLoading: true });
  },
  props: {
    noteId: { type: Number, required: true },
    historyWriter: {
      type: Object as PropType<HistoryWriter>,
      required: true,
    },
  },
  components: {
    LoadingPage,
    NoteArticleView,
    ToolbarFrame,
    Breadcrumb,
    ViewTypeButtons,
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
  },
  methods: {
    noteRealmUpdated(updatedNoteRealm?: Generated.NoteRealm) {
      if (!updatedNoteRealm) {
        this.fetchData();
        return;
      }
      this.noteRealmCache?.updateNoteRealm(updatedNoteRealm);
    },
    async fetchData() {
      const noteWithDescendents =
        await this.api.noteMethods.getNoteWithDescendents(this.noteId);
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
