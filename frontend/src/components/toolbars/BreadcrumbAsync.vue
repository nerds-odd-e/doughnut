<template>
  <ContentLoader v-if="!notePosition" />
  <Breadcrumb v-else v-bind="{ notePosition }" />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { NotePositionViewedByUser } from "@/generated/backend";
import useLoadingApi from "@/managedApi/useLoadingApi";
import ContentLoader from "@/components/commons/ContentLoader.vue";
import Breadcrumb from "./Breadcrumb.vue";

export default defineComponent({
  setup() {
    return { ...useLoadingApi() };
  },
  props: {
    noteId: { type: Number, required: true },
  },
  components: {
    ContentLoader,
    Breadcrumb,
  },
  data() {
    return {
      notePosition: undefined as NotePositionViewedByUser | undefined,
    };
  },
  methods: {
    async fetchData() {
      this.notePosition = await this.managedApi.restNoteController.getPosition(
        this.noteId,
      );
    },
  },

  mounted() {
    this.fetchData();
  },
  watch: {
    noteId() {
      this.fetchData();
    },
  },
});
</script>
