<template>
  <ContainerPage v-bind="{ contentExists: reviewing }">
    <ReviewWelcome v-if="!!reviewing" v-bind="{ reviewing }" />
  </ContainerPage>
</template>

<script>
import ReviewWelcome from "@/components/review/ReviewWelcome.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import timezoneParam from "@/managedApi/window/timezoneParam"
import ContainerPage from "./commons/ContainerPage.vue"

export default {
  setup() {
    return useLoadingApi()
  },
  data() {
    return {
      reviewing: null,
    }
  },
  components: { ReviewWelcome, ContainerPage },
  methods: {
    async fetchData() {
      this.reviewing = await this.managedApi.restReviewsController.overview(
        timezoneParam(),
      )
    },
  },
  mounted() {
    this.fetchData()
  },
}
</script>
