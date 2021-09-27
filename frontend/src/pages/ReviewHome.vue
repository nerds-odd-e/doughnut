<template>
  <LoadingPage v-bind="{ loading, contentExists: !!reviewing }">
    <ReviewWelcome v-if="!!reviewing" v-bind="{ reviewing }" />
  </LoadingPage>
</template>

<script>
import ReviewWelcome from "../components/review/ReviewWelcome.vue";
import LoadingPage from "./commons/LoadingPage.vue";
import { restGet } from "../restful/restful";

export default {
  data() {
    return {
      reviewing: null,
      loading: null,
    };
  },
  components: { ReviewWelcome, LoadingPage },
  methods: {
    fetchData() {
      this.loading = true
      restGet(`/api/reviews/overview`).then(
        (res) => (this.reviewing = res)
      )
      .finally(()=>this.loading = false);
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>
