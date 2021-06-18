<template>
  <LoadingThinBar v-if="loading"/>
  <ReviewWelcome v-if="!!repetition" v-bind="{repetition}"/>
  <div v-else><ContentLoader /></div>
</template>

<script setup>
import ReviewWelcome from '../components/review/ReviewWelcome.vue'
import ContentLoader from "../components/ContentLoader.vue"
import LoadingThinBar from "../components/LoadingThinBar.vue"
import { ref, inject } from 'vue'

const repetition = ref(null)
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  fetch(`/api/reviews/repeat`)
    .then(res => {
      return res.json();
    })
    .then(resp => {
      repetition.value = resp;
      loading.value = false
      if (!repetition.ReviewPointViewedByUser) {
        $router.push({name: "reviews"})
      }
    })
    .catch(error => {
      window.alert(error);
    });
}

fetchData()

</script>
