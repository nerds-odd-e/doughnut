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
    timeToRefresh() {
      const now = new Date();
      return (
        new Date(
          now.getFullYear(),
          now.getMonth(),
          now.getDate(),
          (Math.floor(now.getHours() / 12) + 1) * 12
        ).getTime() - now.getTime()
      );
    },
    fetchData() {
      this.api.reviewMethods.overview().then((res) => (this.reviewing = res));
      this.timer = setTimeout(() => {
        this.fetchData();
      }, this.timeToRefresh());
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
