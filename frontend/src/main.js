import { createApp } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import { createPinia } from "pinia";
import DoughnutApp from './DoughnutApp.vue';
import store from './store';
import routes from './routes/routes';
import 'bootstrap/scss/bootstrap.scss';
import 'bootstrap';

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// to accelerate e2e test
window.router = router;

const app = createApp(DoughnutApp);
app.config.globalProperties.$popups = {};

app.use(router);
app.use(store);
app.use(createPinia);

app.directive('focus', {
  mounted(el) {
    el.querySelector('input, textarea').focus()
  }
})

app.mount('#app');
