<template>
  <LoadingPage v-bind="{ loading, contentExists: true }">
    <div class="inner-box" :key="noteId">
      <div class="content" v-if="noteRealm && noteRealmCache">
        <div class="container">
          <NoteArticleView
            v-bind="{ noteId, noteRealms: noteRealmCache, storageAccessor }"
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
    NoteArticleView,
  },
  data() {
    return {
      noteRealmCache: undefined as NoteRealmCache | undefined,
    };
  },
  computed: {
    noteRealm() {
      return this.noteRealmCache?.getNoteRealmById(this.noteId);
    },
  },
  methods: {
    async fetchData() {
      const noteWithDescendents =
        await this.api.noteMethods.getNoteWithDescendents(this.noteId);
      this.storageAccessor.selectPosition(
        noteWithDescendents.notes[0].note,
        noteWithDescendents.notePosition
      );
      this.noteRealmCache = new NoteRealmCache(noteWithDescendents);
    },
  },
  watch: {
    "storageAccessor.updatedAt": function noteRealmUpdated() {
      if (!this.storageAccessor.updatedNoteRealm) {
        this.fetchData();
        return;
      }
      this.noteRealmCache?.updateNoteRealm(
        this.storageAccessor.updatedNoteRealm
      );
    },

    noteId() {
      this.fetchData();
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>
