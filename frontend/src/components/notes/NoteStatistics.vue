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
        new Date(statistics.note.createdAt).toLocaleString()
      }}</span>
      <label>Last Content Updated:</label>
      <span class="statistics-value">{{
        new Date(statistics.note.textContent.updatedAt).toLocaleString()
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

<script setup>
import useLoadingApi from "../../managedApi/useLoadingApi";
import { ref } from "vue";

const props = defineProps({
  noteId: [String, Number],
  linkid: [String, Number],
});
const statistics = ref(null);
const { api } = useLoadingApi();
const fetchData = () => {
  api.getStatistics(props.noteId, props.linkid).then((articles) => {
    statistics.value = articles;
  });
};

fetchData();
</script>
