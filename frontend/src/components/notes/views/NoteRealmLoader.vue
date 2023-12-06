<template>
  <LoadingPage v-bind="{ contentExists: !!noteRealm }">
    <slot :note-realm="noteRealm" />
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent, PropType, watch, toRefs, reactive } from "vue";
import LoadingPage from "@/pages/commons/LoadingPage.vue";
import { StorageAccessor } from "../../../store/createNoteStorage";

export default defineComponent({
  setup(props) {
    const { noteId, storageAccessor } = toRefs(props);
    const noteRealmObj = reactive({
      noteRealm: storageAccessor.value.refOfNoteRealm(noteId.value),
    });
    watch(
      () => noteId,
      () => {
        throw new Error(
          "NoteCardsView: noteId changed. Please make noteId the key in the parent component.",
        );
      },
    );
    return noteRealmObj;
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
  },
  methods: {
    fetchData() {
      this.storageAccessor
        .storedApi()
        .getNoteRealmAndReloadPosition(this.noteId);
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>
