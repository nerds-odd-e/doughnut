<template>
  <ReviewDoughnutRing v-if="reviewing" :reviewing="reviewing" />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import useLoadingApi from "../../managedApi/useLoadingApi";
import ReviewDoughnutRing from "./ReviewDoughnutRing.vue";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    reviewData: Object,
  },
  data() {
    return {
      reviewing: undefined as undefined | Generated.ReviewStatus,
      timer: null as null | NodeJS.Timeout,
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
    async fetchData() {
      this.reviewing = await this.api.reviewMethods.overview();
      this.timer = setTimeout(this.fetchData, this.timeToRefresh());
    },
  },
  mounted() {
    this.fetchData();
  },
  beforeUnmount() {
    if (this.timer) {
      clearTimeout(this.timer);
    }
  },
  components: { ReviewDoughnutRing },
});
</script>

<style lang="scss" scoped>
.doughnut-ring {
  font-size: 0.8rem;
  font-weight: bold;
  color: #fff;
  background-color: #000;
  padding: 0.2rem 0.5rem;
  border-radius: 0.5rem;
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
}
</style>
