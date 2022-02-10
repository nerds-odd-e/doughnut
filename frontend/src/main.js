import { createApp } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import DoughnutApp from './DoughnutApp.vue';
import store from './store';
import routes from './routes/routes';
import 'bootstrap/scss/bootstrap.scss';
import 'bootstrap';
import { SnackbarPlugin } from 'snackbar-vue';
import "snackbar-vue/dist/snackbar-vue.common.css";

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// to accelerate e2e test
window.router = router;

const app = createApp(DoughnutApp);
app.config.globalProperties.$popups = {};

app.use(SnackbarPlugin, {
  font: { family: 'sans-serif', size: '14px' }
});
app.use(router);
app.use(store);
app.directive('focus', {
  mounted(el) {
    el.querySelector('input, textarea').focus()
  }
})

app.mount('#app');
