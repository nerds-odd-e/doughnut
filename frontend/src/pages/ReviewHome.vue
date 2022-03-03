<template>
  <ContainerPage v-bind="{ loading, contentExists: !!reviewing }">
    <ReviewWelcome v-if="!!reviewing" v-bind="{ reviewing }" />
  </ContainerPage>
</template>

<script>
import ReviewWelcome from "../components/review/ReviewWelcome.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import { api } from "../storedApi";

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
      api().reviewMethods.overview().then(
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
