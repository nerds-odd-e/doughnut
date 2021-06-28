<script>
import {restGet} from "./restful/restful"
import RelativeRouterView from "./routes/RelativeRouterView.vue"

export default {
  components: {RelativeRouterView},
  data() {
    return {
  staticInfo: null,
  loading: null,
    }},
  
  mounted() {
    restGet(`/api/static-info`, (v)=>this.loading = v, (res) => this.staticInfo = res)
  }
}

</script>

<template>
  <RelativeRouterView v-bind="{staticInfo}"></RelativeRouterView>
</template>
