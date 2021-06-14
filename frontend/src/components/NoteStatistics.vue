<template>
  <div v-if="statistics">

  <div v-if="statistics.reviewPoint">
    <label>Repetition Count:</label>
    <span class="statistics-value">{{statistics.reviewPoint.repetitionCount}}</span>
    <label>Forgetting Curive Index:</label>
    <span class="statistics-value">{{statistics.reviewPoint.forgettingCurveIndex}}</span>
    <label>Next Review:</label>
    <span class="statistics-value">{{new Date(statistics.reviewPoint.nextReviewAt).toLocaleString()}}</span>
  </div>

  <div v-if="statistics.note">
    <label>Created:</label>
    <span class="statistics-value">{{new Date(statistics.note.createdDatetime).toLocaleString()}}</span>
    <label>Last Content Updated:</label>
    <span class="statistics-value">{{new Date(statistics.note.noteContent.updatedDatetime).toLocaleString()}}</span>
  </div>

  <div v-if="statistics.link">
    <label>Created:</label>
    <span class="statistics-value">{{new Date(statistics.link.createAt).toLocaleString()}}</span>
  </div>

  </div>
</template>

<script setup>
import { ref, defineProps } from "vue"

const props = defineProps({noteid: Number, linkid: Number})
const statistics = ref(null)
const url = () => {
  if (!!props.noteid) {
    return `/api/notes/${props.noteid}/statistics`
  }
  return `/api/links/${props.linkid}/statistics`
}
const fetchData = async () => {
      fetch(url())
        .then(res => {
          return res.json();
        })
        .then(articles => {
          statistics.value = articles;
        })
        .catch(error => {
          window.alert(error);
        });
    }

fetchData()
</script>
