<template>
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

    <div v-if="statistics.link">
      <label>Created:</label>
      <span class="statistics-value">{{
        new Date(statistics.link.createdAt).toLocaleString()
      }}</span>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    noteId: Number,
    linkid: Number,
  },
  data() {
    return {
      statistics: undefined as undefined | unknown,
    };
  },
  methods: {
    fetchData() {
      this.api.getStatistics(this.noteId, this.linkid).then((articles) => {
        this.statistics = articles;
      });
    },
  },
  mounted() {
    this.fetchData();
  },
});
</script>
