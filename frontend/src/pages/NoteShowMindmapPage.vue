<template>
  <div class="inner-box" :key="noteId">
    <div class="content" v-if="noteRealmCache">
      <div v-bind="{ noteId, storageAccessor }" />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
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
  data() {
    return {
      noteRealmCache: undefined as NoteRealmCache | undefined,
    };
  },
  methods: {
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
