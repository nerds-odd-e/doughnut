<template>
  <div v-if="noteCommentForm">
    This is a future comment form
  </div>
</template>

<script setup>
import { ref } from "vue";

const props = defineProps({ noteId: [String, Number], linkid: [String, Number] });
/*const statistics = ref(null);
const url = () => {
  if (!!props.noteId) {
    return `/api/notes/${props.noteId}/statistics`;
  }
  return `/api/links/${props.linkid}/statistics`;
};
const fetchData = async () => {
  fetch(url())
    .then((res) => {
      return res.json();
    })
    .then((articles) => {
      statistics.value = articles;
    })
    .catch((error) => {
      window.alert(error);
    });
};

fetchData(); */S
</script>
