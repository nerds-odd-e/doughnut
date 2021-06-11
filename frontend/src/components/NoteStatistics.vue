<template>
  <div v-if="statistics">
  <div v-if="statistics.reviewPoint">
    <label>Repetition Count:</label>
    <span class="statistics-value">{{statistics.reviewPoint.repetitionCount}}</span>
    <label>Forgetting Curive Index:</label>
    <span class="statistics-value">{{statistics.reviewPoint.forgettingCurveIndex}}</span>
    <label>Next Review:</label>
    <span class="statistics-value">{{statistics.reviewPoint.nextReviewAt}}</span>
  </div>
  </div>
</template>

<script setup>
import { ref, defineProps } from "vue"

const props = defineProps({noteid: Number})
const statistics = ref(null)
const fetchData = async () => {
      fetch(`/api/notes/${props.noteid}/statistics`)
        .then(res => {
          return res.json();
        })
        .then(articles => {
          statistics.value = articles;
        })
        .catch(error => {
          alert(error);
        });
    }

fetchData()
</script>
