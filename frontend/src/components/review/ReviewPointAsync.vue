<template>
  <LoadingPage v-bind="{ loading, contentExists: !!reviewPointViewedByUser }">
    <ShowReviewPoint
      v-bind="{
        reviewPointViewedByUser
      }"
      :key="reviewPointId"

      />

  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import LoadingPage from '../../pages/commons/LoadingPage.vue';
import ShowReviewPoint from './ShowReviewPoint.vue';

export default defineComponent({
  setup() {
    return useStoredLoadingApi({initalLoading: true});
  },
  props: {
     reviewPointId: { type: Number, required: true },
  },
  components: { LoadingPage, ShowReviewPoint },
  data() {
    return {
      reviewPointViewedByUser: undefined as Generated.ReviewPointViewedByUser | undefined
    }
  },
  methods: {
    async fetchData() {
      this.reviewPointViewedByUser = this.storedApi.reviewMethods.getReviewPoint(this.reviewPointId)
    },
  },
  watch: {
    reviewPointId() {
      this.fetchData();
    },
  },
  mounted() {
    this.fetchData();
  },
});

</script>