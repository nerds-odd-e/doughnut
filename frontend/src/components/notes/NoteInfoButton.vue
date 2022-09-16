<template>
  <span
    v-if="!noteInfo"
    @click="toggleNoteInfo()"
    class="info-button"
    role="button"
    title="note info"
    width="100%"
  >
    i...
  </span>
  <LoadingPage v-bind="{ contentExists: true }">
    <NoteInfo
      v-if="noteInfo"
      :note-info="noteInfo"
      @level-changed="$emit('levelChanged', $event)"
      @self-evaluated="$emit('selfEvaluated', $event)"
    />
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import LoadingPage from "@/pages/commons/LoadingPage.vue";
import NoteInfo from "./NoteInfo.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: { noteId: { type: Number, required: true }, expanded: Boolean },
  emits: ["levelChanged", "selfEvaluated"],
  components: { LoadingPage, NoteInfo },
  data() {
    return { noteInfo: undefined as undefined | Generated.NoteInfo };
  },
  methods: {
    fetchData() {
      this.api.getNoteInfo(this.noteId).then((articles) => {
        this.noteInfo = articles;
      });
    },

    toggleNoteInfo() {
      if (!this.noteInfo) {
        this.fetchData();
      } else {
        this.noteInfo = undefined;
      }
    },
  },
  mounted() {
    if (this.expanded) {
      this.fetchData();
    }
  },
});
</script>

<style lang="scss" scoped>
.info-button {
  width: 100%;
  align-items: center;
  justify-content: center;
  font-size: smaller;
  cursor: pointer;
  display: block;
}
</style>
