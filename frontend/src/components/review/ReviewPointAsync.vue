<template>
  <ContentLoader v-if="!reviewPoint" />
  <main v-else>
    <Breadcrumb v-bind="{ noteTopic: reviewPoint.note.noteTopic }" />
    <NoteShow
      v-bind="{
        noteId: reviewPoint.note.id,
        expandChildren: false,
        readonly: false,
        storageAccessor,
      }"
    />
  </main>
  />
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { ReviewPoint } from "@/generated/backend";
import useLoadingApi from "@/managedApi/useLoadingApi";
import { StorageAccessor } from "@/store/createNoteStorage";
import ContentLoader from "@/components/commons/ContentLoader.vue";
import NoteShow from "../notes/NoteShow.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    reviewPointId: { type: Number, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: {
    ContentLoader,
    NoteShow,
  },
  data() {
    return {
      reviewPoint: undefined as ReviewPoint | undefined,
    };
  },
  methods: {
    async fetchData() {
      this.reviewPoint = await this.managedApi.restReviewPointController.show(
        this.reviewPointId,
      );
    },
  },
  watch: {
    reviewPointId() {
      this.fetchData();
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>
