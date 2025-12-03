import { createApp, nextTick } from "vue"
import { createRouter, createWebHistory } from "vue-router"
import Toast from "vue-toastification/dist/index.mjs"
import "vue-toastification/dist/index.css"

import routes from "./routes/routes"
import "./assets/daisyui.css"
import DoughnutAppVue from "./DoughnutApp.vue"

const router = createRouter({
  history: createWebHistory("/"),
  routes,
})

// to accelerate e2e test
Object.assign(window, { router })

const app = createApp(DoughnutAppVue)

app.use(router)

app.use(Toast, {
  position: "top-right",
  timeout: 3000,
  closeOnClick: true,
  pauseOnFocusLoss: true,
  pauseOnHover: true,
  draggable: true,
  draggablePercent: 0.6,
  showCloseButtonOnHover: false,
  hideProgressBar: true,
  closeButton: "button",
  icon: true,
  rtl: false,
})

app.directive("focus", {
  mounted(el) {
    const focusInput = () => {
      const input = el.querySelector("input, textarea")
      if (input) {
        ;(input as HTMLInputElement | HTMLTextAreaElement).focus()
      }
    }
    // Use nextTick to ensure DOM is fully rendered
    nextTick(() => {
      focusInput()
    })
  },
  updated(el) {
    const focusInput = () => {
      const input = el.querySelector("input, textarea")
      if (input) {
        ;(input as HTMLInputElement | HTMLTextAreaElement).focus()
      }
    }
    // Use nextTick to ensure DOM is fully rendered after update
    nextTick(() => {
      focusInput()
    })
  },
})

app.mount("#app")
