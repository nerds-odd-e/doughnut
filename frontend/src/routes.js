import {createRouter,createWebHistory} from 'vue-router'
import TempApp from './pages/TempApp.vue'
import ReviewHome from './pages/ReviewHome.vue'
import Repeat from './pages/Repeat.vue'

const routes = [
    { path: '/notes/:noteid', name: 'noteShow', component: TempApp, props: true },
    { path: '/bazaar/notes/:noteid', name: 'bnoteShow', component: TempApp, props: true },
    { path: '/reviews', name: 'reviews', component: ReviewHome },
    { path: '/reviews/repeat', name: 'repeat', component: Repeat },
  ]
  
const router = createRouter({
    history: createWebHistory(),
    routes,
})

export {router}