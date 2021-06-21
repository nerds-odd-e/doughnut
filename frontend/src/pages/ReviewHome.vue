<template>
  <LoadingThinBar v-if="loading"/>
  <ReviewWelcome v-if="!!reviewing" v-bind="{reviewing}"/>
  <div v-else><ContentLoader /></div>
</template>

<script setup>
import ReviewWelcome from '../components/review/ReviewWelcome.vue'
import ContentLoader from "../components/ContentLoader.vue"
import LoadingThinBar from "../components/LoadingThinBar.vue"
import {restGet} from "../restful/restful"
import { ref, inject } from 'vue'

const reviewing = ref(null)
const loading = ref(false)

const fetchData = async () => {
  restGet(`/api/reviews/overview`, loading, (res)=>reviewing.value = res)
}

fetchData()

</script>
