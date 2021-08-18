import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import DoughnutApp from './DoughnutApp.vue'
import {routes } from './routes/routes'
import {colors } from './colors'

  
const router = createRouter({
    history: createWebHistory(),
    routes,
})

const app = createApp(DoughnutApp)
app.config.globalProperties.$staticInfo = {
    colors: colors 
}
app.config.globalProperties.$popups = {}

app.use(router)
app.mount('#partials-noteshow')