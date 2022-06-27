<template>
  <ContainerPage
    v-bind="{
      loading,
      contentExists: reviewPoints !== undefined,
    }"
  >
    <InitialReview v-if="reviewPoints" v-bind="{ nested, reviewPoints }" />
  </ContainerPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import ContainerPage from "./commons/ContainerPage.vue";
import InitialReview from "../components/review/InitialReview.vue";
import useLoadingApi from "../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return { ...useLoadingApi() };
  },
  props: { nested: Boolean },
  components: {
    ContainerPage,
    InitialReview,
  },
  data() {
    return {
      reviewPoints: undefined as Generated.ReviewPoint[] | undefined,
    };
  },

  mounted() {
    this.api.reviewMethods.initialReview().then((resp) => {
      if (resp.length === 0) {
        this.$router.push({ name: "reviews" });
        return;
      }
      this.reviewPoints = resp;
    });
  },
});
</script>
