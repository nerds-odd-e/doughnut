<template>
  <ContainerPage v-bind="{ contentExists: reviewing }">
    <ReviewWelcome v-if="!!reviewing" v-bind="{ reviewing }" />
  </ContainerPage>
</template>

<script>
import ReviewWelcome from "../components/review/ReviewWelcome.vue";
import ContainerPage from "./commons/ContainerPage.vue";
import useLoadingApi from "../managedApi/useLoadingApi";

export default {
  setup() {
    return useLoadingApi();
  },
  data() {
    return {
      reviewing: null,
    };
  },
  components: { ReviewWelcome, ContainerPage },
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
