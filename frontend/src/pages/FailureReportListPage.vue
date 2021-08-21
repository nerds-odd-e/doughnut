<template>
  <h2>Failure Reports</h2>

  <LoadingPage v-bind="{loading, contentExists: !!failureReports}">
    <div v-if="!!failureReports">
      <div class="failure-report" v-for="element in failureReports" :key="element.id">
        {{element.createDatetime}} :
        <router-link :to="{name: 'failureReport', params: { failureReportId: element.id}}">
            {{element.errorName}}
        </router-link>
      </div>
    </div>
  </LoadingPage>
</template>

<script>
import LoadingPage from "./commons/LoadingPage.vue"
import {restGet} from "../restful/restful"

export default {
  components: { LoadingPage },
  data() {
    return {
      loading: false,
      failureReports: null,
    }
  },
  methods: {
    fetchData() {
      restGet(`/api/failure-reports`, r=>this.loading=r)
        .then( res => this.failureReports = res)
    }
  },
  mounted() {
    this.fetchData()
  }
}
</script>
