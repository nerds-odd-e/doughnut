import {createRouter,createWebHistory} from 'vue-router'
import NoteShowPage from './pages/NoteShowPage.vue'
import NoteNewPage from './pages/NoteNewPage.vue'
import NoteEditPage from './pages/NoteEditPage.vue'
import LinkShowPage from './pages/LinkShowPage.vue'
import ReviewHome from './pages/ReviewHome.vue'
import Repeat from './pages/Repeat.vue'
import InitialReview from './pages/InitialReview.vue'

const routes = [
    { path: '/notes/:noteid', name: 'noteShow', component: NoteShowPage, props: true },
    { path: '/bazaar/notes/:noteid', name: 'bnoteShow', component: NoteShowPage, props: true },
    { path: '/notes/:noteid/new', name: 'noteNew', component: NoteNewPage, props: true },
    { path: '/notes/:noteid/edit', name: 'noteEdit', component: NoteEditPage, props: true },
    { path: '/links/:linkid', name: 'linkShow', component: LinkShowPage, props: true },
    { path: '/reviews', name: 'reviews', component: ReviewHome },
    { path: '/reviews/initial', name: 'initial', component: InitialReview },
    { path: '/reviews/repeat', name: 'repeat', component: Repeat },
  ]
  
const router = createRouter({
    history: createWebHistory(),
    routes,
})

export {router}