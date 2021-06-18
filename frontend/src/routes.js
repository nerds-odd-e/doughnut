import {createRouter,createWebHistory} from 'vue-router'
import TempApp from './pages/TempApp.vue'
import ReviewHome from './pages/ReviewHome.vue'

const routes = [
    { path: '/notes/:noteid', name: 'noteShow', component: TempApp, props: true },
    { path: '/bazaar/notes/:noteid', name: 'bnoteShow', component: TempApp, props: true },
    { path: '/reviews', name: 'reviews', component: ReviewHome },
  ]
  
const router = createRouter({
    history: createWebHistory(),
    routes,
})

export {router}