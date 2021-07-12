<script>
import {restGet} from "./restful/restful"
import Popups from "./components/commons/Popups.vue"

export default {
  data() {
    return {
      staticInfo: null,
      loading: null,
      popupInfo: [],
      doneResolve: null,
    }},

  components: { Popups },

  methods: {
    done(result) {
      this.doneResolve(result)
      this.popupInfo = null
      this.doneResolve = null
    }
  },
  
  mounted() {
    restGet(`/api/static-info`, (v)=>this.loading = v)
      .then((res) => {
      this.staticInfo = res
      Object.assign(this.$staticInfo, res)
      })

    this.$popups.alert = msg => {
      this.popupInfo = { type: "alert", message: msg }
      return new Promise((resolve, reject) => { this.doneResolve = resolve})
    }

    this.$popups.confirm = msg => {
      this.popupInfo = { type: "confirm", message: msg }
      return new Promise((resolve, reject) => { this.doneResolve = resolve})
    }

    this.$popups.dialog = (component, attrs) => {
      this.popupInfo = { type: "dialog", component, attrs }
      return new Promise((resolve, reject) => { this.doneResolve = resolve})
    }

  }
}

</script>

<template>
  <Popups :popupInfo="popupInfo" @done="done($event)"/>
  <router-view />
</template>
