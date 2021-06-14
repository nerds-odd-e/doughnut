<template>
  <div v-if="note">
  <!-- <div th:fragment="noteShow(note, level, forBazaar)"> -->
      <!-- th:replace=":: noteShowWithTitle(${note}, ${level}, ${forBazaar}, ~{:: .note-title-show}, ~{:: .note-body-show})"> -->
      <h2 :class="'h' + level"> {{note.noteContent.title}}</h2>
      <pre class="note-body" style="white-space: pre-wrap;">{{note.noteContent.description}}</pre>
  </div>
</template>

<script setup>
import { ref, defineProps } from "vue"

const props = defineProps({noteid: Number, level: Number, forBazaar: Boolean})
const note = ref(null)
const url = () => {
  return `/api/notes/${props.noteid}`
}
const fetchData = async () => {
      fetch(url())
        .then(res => {
          return res.json();
        })
        .then(articles => {
          note.value = articles;
        })
        .catch(error => {
          window.alert(error);
        });
    }

fetchData()
</script>
