<template>
  <LoadingPage v-bind="{ loading, contentExists: true }">
    <div v-if="statistics">
      <div v-if="statistics.reviewPoint">
        <label>Repetition Count:</label>
        <span class="statistics-value">{{
          statistics.reviewPoint.repetitionCount
        }}</span>
        <label>Forgetting Curive Index:</label>
        <span class="statistics-value">{{
          statistics.reviewPoint.forgettingCurveIndex
        }}</span>
        <label>Next Review:</label>
        <span class="statistics-value">{{
          new Date(statistics.reviewPoint.nextReviewAt).toLocaleString()
        }}</span>
      </div>

      <div v-if="statistics.note">
        <label>Created:</label>
        <span class="statistics-value">{{
          new Date(statistics.createdAt).toLocaleString()
        }}</span>
        <label>Last Content Updated:</label>
        <span class="statistics-value">{{
          new Date(statistics.note.note.textContent.updatedAt).toLocaleString()
        }}</span>
      </div>
      <ReviewSettingForm
        v-bind="{ noteId, reviewSetting }"
        @level-changed="$emit('levelChanged', $event)"
      />
    </div>
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import LoadingPage from "@/pages/commons/LoadingPage.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import ReviewSettingForm from "../review/ReviewSettingForm.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    noteId: { type: Number, required: true },
  },
  emits: ["levelChanged"],
  data() {
    return {
      statistics: undefined as undefined | Generated.NoteStatistics,
    };
  },
  computed: {
    reviewSetting() {
      return this.statistics?.reviewSetting;
    },
  },
  methods: {
    fetchData() {
      this.api.getStatistics(this.noteId).then((articles) => {
        this.statistics = articles;
      });
    },
  },
  mounted() {
    this.fetchData();
  },
  components: { LoadingPage, ReviewSettingForm },
});
</script>
