import { createApp } from "vue";
import { createRouter, createWebHistory } from "vue-router";

import routes from "./routes/routes";
import "bootstrap/scss/bootstrap.scss";
import "bootstrap";
import DoughnutAppVue from "./DoughnutApp.vue";
import loginOrRegisterAndHaltThisThread from "./managedApi/window/loginOrRegisterAndHaltThisThread";

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// to accelerate e2e test
Object.assign(window, { router });

const app = createApp(DoughnutAppVue);

app.use(router);

app.directive("focus", {
  mounted(el) {
    el.querySelector("input, textarea").focus();
  },
});

window.onunhandledrejection = (event: PromiseRejectionEvent) => {
  // eslint-disable-next-line
  if (event.reason === "Unauthorized") {
    loginOrRegisterAndHaltThisThread();
    return;
  }
  if (event.reason === "Unauthorized non-GET request") {
    if (
      // eslint-disable-next-line no-alert
      window.confirm(
        "You are logged out. Do you want to log in (and lose the current changes)?",
      )
    ) {
      loginOrRegisterAndHaltThisThread();
      return;
    }
  }
  throw event.reason;
};

app.mount("#app");
