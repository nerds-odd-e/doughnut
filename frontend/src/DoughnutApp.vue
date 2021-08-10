<script>
import {restGet} from "./restful/restful"
import Popups from "./components/commons/Popups.vue"
import MainMenu from "./components/commons/MainMenu.vue"

export default {
  data() {
    return {
      showNavBar: true,
      staticInfo: null,
      loading: null,
      popupInfo: [],
      doneResolve: null,
    }},

  components: { Popups, MainMenu },

  watch: {
    $route(to, from) {
      if(to.name) {
        this.showNavBar = !['repeat', 'initial'].includes(to.name.split('-').shift())
      }
    }
  },

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
  <MainMenu v-if="showNavBar" :user="staticInfo ? staticInfo.user : null"/>
  <div class="container content">
    <router-view />
  </div>
</template>
