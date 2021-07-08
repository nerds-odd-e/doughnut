<script>
import {restGet} from "./restful/restful"
import Popups from "./components/commons/Popups.vue"

export default {
  data() {
    return {
      staticInfo: null,
      loading: null,
      popupInfo: null,
      doneResolve: null,
    }},

  components: { Popups },

  methods: {
    done(result) {
      console.log(result)

      this.doneResolve(result)
      this.popupInfo = null
      this.doneResolve = null
    }
  },
  
  mounted() {
    restGet(`/api/static-info`, (v)=>this.loading = v, (res) => {
      this.staticInfo = res
      Object.assign(this.$staticInfo, res)
    })

    this.$popups.confirm = msg => {
      this.popupInfo = { type: "confirm", message: msg }
      this.popupPromise = new Promise((resolve, reject) => { this.doneResolve = resolve})
      return this.popupPromise
    }
  }
}

</script>

<template>
  <Popups :popupInfo="popupInfo" :popUpPromise="popupPromise" @done="done($event)"/>
  <router-view />
</template>
