<template>
  <LoadingPage v-bind="{loading, contentExists: !!reviewing}">
    <ReviewWelcome v-if="!!reviewing" v-bind="{reviewing}"/>
  </LoadingPage>
</template>

<script setup>
import ReviewWelcome from '../components/review/ReviewWelcome.vue'
import LoadingPage from "./commons/LoadingPage.vue"
import {restGet} from "../restful/restful"
import { ref, inject } from 'vue'

const reviewing = ref(null)
const loading = ref(false)

const fetchData = async () => {
  restGet(`/api/reviews/overview`, loading, (res)=>reviewing.value = res)
}

fetchData()

</script>
