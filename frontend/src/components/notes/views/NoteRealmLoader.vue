<template>
  <LoadingPage v-bind="{ contentExists: !!noteRealm }">
    <slot :note-realm="noteRealm" />
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent, PropType, watch } from "vue";
import LoadingPage from "@/pages/commons/LoadingPage.vue";
import { StorageAccessor } from "../../../store/createNoteStorage";

export default defineComponent({
  setup(props) {
    watch(
      () => props.noteId,
      () => {
        throw new Error(
          "NoteCardsView: noteId changed. Please make noteId the key in the parent component.",
        );
      },
    );
    // eslint-disable-next-line vue/no-setup-props-destructure
    const { noteId } = props;
    return { noteRealm: props.storageAccessor.refOfNoteRealm(noteId) };
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
        .api(this.$router)
        .getNoteRealmAndReloadPosition(this.noteId);
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>
