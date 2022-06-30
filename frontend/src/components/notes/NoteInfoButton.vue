<template>
  <span
    v-if="!noteInfo"
    @click="toggleStatistics()"
    class="info-button"
    role="button"
    title="statistics"
    width="100%"
  >
    i...
  </span>
  <LoadingPage v-bind="{ loading, contentExists: true }">
    <NoteInfo
      v-if="noteInfo"
      :note-info="noteInfo"
      @level-changed="$emit('levelChanged', $event)"
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
  emits: ["levelChanged"],
  components: { LoadingPage, NoteInfo },
  data() {
    return { noteInfo: undefined as undefined | Generated.NoteInfo };
  },
  methods: {
    fetchData() {
      this.api.getStatistics(this.noteId).then((articles) => {
        this.noteInfo = articles;
      });
    },

    toggleStatistics() {
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
