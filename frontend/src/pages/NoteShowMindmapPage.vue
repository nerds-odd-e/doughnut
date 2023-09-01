<template>
  <div class="inner-box" :key="noteId">
    <div class="content" v-if="noteRealm && noteRealmCache">
      <NoteMindmapView
        v-bind="{ noteId, noteRealms: noteRealmCache, storageAccessor }"
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
    async fetchData() {
      this.noteRealmCache = new NoteRealmCache(
        await this.api.noteMethods.getNoteWithDescendents(this.noteId),
      );
    },
  },
  watch: {
    "storageAccessor.updatedNoteRealm": function updateAt() {
      this.noteRealmCache?.updateNoteRealm(
        this.storageAccessor.updatedNoteRealm,
      );
    },
    noteId() {
      this.fetchData();
    },
  },
  async mounted() {
    await this.fetchData();
  },
});
</script>
