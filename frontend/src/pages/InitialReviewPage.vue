<template>
  <ContainerPage
    v-bind="{ loading, contentExists: reviewPointViewedByUsers !== undefined }"
  >
    <InitialReview
      v-if="reviewPointViewedByUsers"
      v-bind="{ nested, reviewPointViewedByUsers }"
    />
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import ContainerPage from "./commons/ContainerPage.vue";
import InitialReview from "../components/review/InitialReview.vue";
import useStoredLoadingApi from "../managedApi/useStoredLoadingApi";

export default defineComponent({
  setup() {
    return { ...useStoredLoadingApi() };
  },
  props: { nested: Boolean },
  components: {
    ContainerPage,
    InitialReview,
  },
  data() {
    return {
      reviewPointViewedByUsers: undefined as
        | Generated.ReviewPointViewedByUser[]
        | undefined,
    };
  },

  mounted() {
    this.storedApi.reviewMethods.initialReview().then((resp) => {
      if (resp.length === 0) {
        this.$router.push({ name: "reviews" });
        return;
      }
      this.reviewPointViewedByUsers = resp;
    });
  },
});
</script>
