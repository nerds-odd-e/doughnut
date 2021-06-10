<template>
  <div >
    Coming soon... {{bbb}}
  </div>
</template>

<script setup>
import { ref, onMounted, defineProps } from "vue"

const props = defineProps({noteid: Number})
const bbb = ref({
    bbb: '1',
    })
const fetchData = () => {
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

onMounted(fetchData);
</script>
