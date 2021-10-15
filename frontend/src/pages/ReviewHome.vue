<template>
  <Containerpage v-bind="{ loading, contentExists: !!reviewing }">
    <ReviewWelcome v-if="!!reviewing" v-bind="{ reviewing }" />
  </Containerpage>
</template>

<script>
import ReviewWelcome from "../components/review/ReviewWelcome.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import { restGet } from "../restful/restful";

export default {
  data() {
    return {
      reviewing: null,
      loading: null,
    };
  },
  components: { ReviewWelcome, ContainerPage },
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
