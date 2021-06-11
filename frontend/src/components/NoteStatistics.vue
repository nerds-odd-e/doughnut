<template>
  <div >
    Coming soon... {{bbb}}
    <div v-for="(value, name) in bbb">
      <label>{{ name }}</label>: <span>{{ value }} </span>
    </div>
  </div>
</template>

<script setup>
import { ref, defineProps } from "vue"

const props = defineProps({noteid: Number})
const bbb = ref({
    bbb: '1',
    })
const fetchData = async () => {
      fetch(`/api/notes/${props.noteid}/statistics`)
        .then(res => {
          return res.json();
        })
        .then(articles => {
          bbb.value = articles;
        })
        .catch(error => {
          alert(error);
        });
    }

fetchData()
</script>
