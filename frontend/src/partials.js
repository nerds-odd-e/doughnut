import { createApp } from 'vue'
import {createRouter,createWebHistory} from 'vue-router'
import Partials from './Partials.vue'
import TempApp from './TempApp.vue'
import NoteApp from './NoteApp.vue'

const partialApp = createApp(Partials);
const partialEl = document.getElementById('partials');
if (partialEl) {
    partialApp.provide('noteid', partialEl.dataset.noteid)
    partialApp.provide('linkid', partialEl.dataset.linkid)
    partialApp.mount('#partials')
}

const routes = [
    { path: '/notes/:noteid', component: TempApp, props: true },
  ]
  
const router = createRouter({
    history: createWebHistory(),
    routes,
})

const app = createApp(NoteApp)
app.use(router)
app.mount('#partials-noteshow')
