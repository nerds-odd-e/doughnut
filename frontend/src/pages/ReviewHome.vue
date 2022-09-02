<template>
  <ContainerPage v-bind="{ loading, contentExists: !!reviewing }">
    <ReviewDoughnutRing v-if="reviewing" :reviewing="reviewing" />
    <ReviewWelcome v-if="!!reviewing" v-bind="{ reviewing }" />
  </ContainerPage>
</template>

<script>
import ReviewWelcome from "../components/review/ReviewWelcome.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import useLoadingApi from "../managedApi/useLoadingApi";
import ReviewDoughnutRing from "../components/review/ReviewDoughnutRing.vue";

export default {
  setup() {
    return useLoadingApi();
  },
  data() {
    return {
      reviewing: null,
    };
  },
  components: { ReviewWelcome, ContainerPage, ReviewDoughnutRing },
  methods: {
    fetchData() {
      this.api.reviewMethods.overview().then((res) => (this.reviewing = res));
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>
