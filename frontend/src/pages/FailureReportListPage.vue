<template>

    <p v-if="!!errorMessage" v-text="errorMessage"></p>
  <LoadingPage v-bind="{loading, contentExists: !!failureReports}">
    <div v-if="!!failureReports">
      <h2>Failure report list</h2>
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
import {restGet, loginOrRegister} from "../restful/restful"

export default {
  props: { user: Object },
  components: { LoadingPage },
  data() {
    return {
      loading: false,
      failureReports: null,
      errorMessage: null,
    }
  },
  methods: {
    fetchData() {
      restGet(`/api/failure-reports`, r=>this.loading=r)
        .then( res => this.failureReports = res)
        .catch(()=> this.errorMessage = "It seems you cannot access this page.")
    }
  },
  mounted() {
    this.fetchData()
  },
  beforeRouteEnter(to, from, next) {
    next(vm=>{
      if(!vm.user) {
        loginOrRegister()
        next(false)
      }
      next()
    })
  },

}
</script>
