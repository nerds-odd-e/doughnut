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
  emits: ["updateUser"],
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
    async logout() {
      await this.api.userMethods.logout();
      window.location.href = "/bazaar";
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
