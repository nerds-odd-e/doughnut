<script>
import {restGet} from "./restful/restful"

export default {
  data() {
    return {
      staticInfo: null,
      loading: null,
    }},
  
  mounted() {
    restGet(`/api/static-info`, (v)=>this.loading = v, (res) => {
      this.staticInfo = res
      Object.assign(this.$staticInfo, res)
    })
  }
}

</script>

<template>
  <router-view />
</template>
