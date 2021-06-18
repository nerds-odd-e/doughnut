<template>
  <LoadingThinBar v-if="loading"/>
  <ReviewWelcome v-if="!!reviewing" v-bind="{reviewing}"/>
  <div v-else><ContentLoader /></div>
</template>

<script setup>
import ReviewWelcome from '../components/review/ReviewWelcome.vue'
import ContentLoader from "../components/ContentLoader.vue"
import LoadingThinBar from "../components/LoadingThinBar.vue"
import { ref, inject } from 'vue'

const reviewing = ref(null)
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  fetch(`/api/reviews/overview`)
    .then(res => {
      return res.json();
    })
    .then(resp => {
      reviewing.value = resp;
      loading.value = false
    })
    .catch(error => {
      window.alert(error);
    });
}

fetchData()

</script>
