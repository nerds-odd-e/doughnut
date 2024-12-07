import { createApp } from "vue"
import { createRouter, createWebHistory } from "vue-router"

import routes from "./routes/routes"
import "bootstrap/scss/bootstrap.scss"
// begin to migrate to DaisyUI
import "./assets/daisyui.css"
import "bootstrap"
import DoughnutAppVue from "./DoughnutApp.vue"

const router = createRouter({
  history: createWebHistory("/"),
  routes,
})

// to accelerate e2e test
Object.assign(window, { router })

const app = createApp(DoughnutAppVue)

app.use(router)

app.directive("focus", {
  mounted(el) {
    el.querySelector("input, textarea").focus()
  },
})

app.mount("#app")
