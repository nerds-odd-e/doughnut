<template>
  <LoadingPage v-bind="{loading, contentExists: !!reviewing}">
    <ReviewWelcome v-if="!!reviewing" v-bind="{reviewing}"/>
  </LoadingPage>
</template>

<script>
import ReviewWelcome from '../components/review/ReviewWelcome.vue'
import LoadingPage from "./commons/LoadingPage.vue"
import {restGet} from "../restful/restful"

export default {
  data() {
    return {
      reviewing: null,
      loading: null
    }
  },
  components: { ReviewWelcome, LoadingPage },
  methods: {
    fetchData() {
      restGet(`/api/reviews/overview`, r=>this.loading=r)
        .then(res=>this.reviewing = res)
    }
  },
  mounted() {
    this.fetchData()
  }
}

</script>
