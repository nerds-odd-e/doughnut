<script>
import {restGet} from "./restful/restful"

export default {
  data() {
    return {
  staticInfo: null,
  loading: null,
    }},
  
  methods: {
    redirectTo(params) { this.$router.push(params) },
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
