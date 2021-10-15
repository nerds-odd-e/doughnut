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
import { restGet, loginOrRegister } from "../restful/restful";

export default {
  props: { user: Object },
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
      this.loading = true
      restGet(`/api/failure-reports`)
        .then((res) => {
          this.failureReports = res
        })
        .catch(
          () => (this.errorMessage = "It seems you cannot access this page.")
        )
        .finally(() => this.loading = false)
    },
  },
  mounted() {
    this.fetchData();
  },
  beforeRouteEnter(to, from, next) {
    next((vm) => {
      if (!vm.user) {
        loginOrRegister();
        next(false);
      }
      next();
    });
  },
};
</script>
