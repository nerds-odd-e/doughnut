<template>
  <div class="inner-box" :key="noteId">
    <div class="content" v-if="noteRealm && noteRealmCache">
      <NoteMindmapView
        v-bind="{ noteId, noteRealms: noteRealmCache, storageAccessor }"
        :highlight-note-id="selectedNoteId"
        @select-note="highlight($event)"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteMindmapView from "../components/notes/views/NoteMindmapView.vue";
import useLoadingApi from "../managedApi/useLoadingApi";
import NoteRealmCache from "../store/NoteRealmCache";
import { StorageAccessor } from "../store/createNoteStorage";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    noteId: { type: Number, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
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
  },
  methods: {
    onNoteDeleted(deletedNoteId: Doughnut.ID) {
      this.noteRealmCache?.deleteNoteAndDescendents(deletedNoteId);
    },
    noteRealmUpdated(updatedNoteRealm?: Generated.NoteRealm) {
      this.noteRealmCache?.updateNoteRealm(updatedNoteRealm);
    },
    highlight(id: Doughnut.ID) {
      this.selectedNoteId = id;
      this.storageAccessor.selectPosition(
        this.noteRealmCache?.getNoteRealmById(this.selectedNoteId)?.note,
        this.noteRealmCache?.getNotePosition(this.selectedNoteId)
      );
    },
    async fetchData() {
      this.noteRealmCache = new NoteRealmCache(
        await this.api.noteMethods.getNoteWithDescendents(this.noteId)
      );
    },
  },
  watch: {
    "storageAccessor.storageUpdatedAt": function updateAt() {
      this.noteRealmUpdated(this.storageAccessor.updatedNoteRealm);
    },
    noteId() {
      this.fetchData();
    },
  },
  async mounted() {
    await this.fetchData();
    this.highlight(this.noteId);
  },
});
</script>
