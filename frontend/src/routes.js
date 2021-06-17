import {createRouter,createWebHistory} from 'vue-router'
import TempApp from './TempApp.vue'

const routes = [
    { path: '/notes/:noteid', name: 'noteShow', component: TempApp, props: true },
    { path: '/bazaar/notes/:noteid', name: 'bnoteShow', component: TempApp, props: true },
  ]
  
const router = createRouter({
    history: createWebHistory(),
    routes,
})

export {router}