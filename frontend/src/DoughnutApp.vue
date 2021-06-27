<script>
import {restGet} from "./restful/restful"
import {relativeRoute} from "./relative_routes"

export default {
  data() {
    return {
  staticInfo: null,
  loading: null,
    }},
  
  methods: {
    redirectTo(params) { this.$router.push(relativeRoute(this.$router, params)) },
    fetchStaticInfo() {
    },
  },

  mounted() {
      restGet(`/api/static-info`, (v)=>this.loading = v, (res) => this.staticInfo = res)
  }
}

</script>

<template>
  <router-view @redirect="redirectTo($event)" v-bind="{staticInfo}"></router-view>
</template>
