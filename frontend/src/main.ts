import { createApp } from "vue";
import { createRouter, createWebHistory } from "vue-router";
import { library } from "@fortawesome/fontawesome-svg-core";
import { FontAwesomeIcon } from "@fortawesome/vue-fontawesome";
import { faFaceSmile, faFaceFrown } from "@fortawesome/free-solid-svg-icons";

import routes from "./routes/routes";
import "bootstrap/scss/bootstrap.scss";
import "bootstrap";
import DoughnutAppVue from "./DoughnutApp.vue";

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// to accelerate e2e test
Object.assign(window, { router });

library.add(faFaceSmile, faFaceFrown);

const app = createApp(DoughnutAppVue).component(
  "font-awesome-icon",
  FontAwesomeIcon,
);

app.use(router);

app.directive("focus", {
  mounted(el) {
    el.querySelector("input, textarea").focus();
  },
});

app.mount("#app");
