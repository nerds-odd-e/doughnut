<template>
  <p v-if="!!errorMessage" v-text="errorMessage"></p>
  <ContainerPage v-bind="{ loading, contentExists: !!failureReports }">
    <div v-if="!!failureReports">
      <h2>Failure report list</h2>
      <div
        class="failure-report"
        v-for="element in failureReports"
        :key="element.id"
      >
        {{ element.createDatetime }} :
        <router-link
          :to="{
            name: 'failureReport',
            params: { failureReportId: element.id },
          }"
        >
          {{ element.errorName }}
        </router-link>
      </div>
    </div>
  </ContainerPage>
</template>

<script>
import ContainerPage from "./commons/ContainerPage.vue";
import api from  "../managedApi/api";

export default {
  components: { ContainerPage },
  data() {
    return {
      loading: true,
      failureReports: null,
      errorMessage: null,
    };
  },
  methods: {
    fetchData() {
      api(this).getFailureReports()
        .then((res) => {
          this.failureReports = res
        })
        .catch(
          () => (this.errorMessage = "It seems you cannot access this page.")
        )
    },
  },
  mounted() {
    this.fetchData();
  },
  computed: {
    user() { return this.$store.getters.getCurrentUser()},
  },
};
</script>
