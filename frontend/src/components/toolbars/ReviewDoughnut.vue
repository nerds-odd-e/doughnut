<template>
  <div v-if="reviewing" v-bind="{ reviewing }">
    <span class="doughnut-initial-reviews">
      {{ `${reviewing.toInitialReviewCount}/${reviewing.notLearntCount}` }}
    </span>
  </div>
</template>

<script>
import useLoadingApi from "../../managedApi/useLoadingApi";

export default {
  setup() {
    return useLoadingApi();
  },
  props: {
    reviewData: Object,
  },
  data() {
    return {
      reviewing: null,
      timer: null,
    };
  },
  methods: {
    fetchData() {
      this.api.reviewMethods.overview().then((res) => (this.reviewing = res));
      this.timer = setTimeout(() => {
        this.fetchData();
      }, 12 * 60 * 60 * 1000);
    },
  },
  mounted() {
    this.fetchData();
  },
  beforeUnmount() {
    clearTimeout(this.timer);
  },
};
</script>
