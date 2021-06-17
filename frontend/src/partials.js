import { createApp } from 'vue'
import Partials from './Partials.vue'
import NoteApp from './NoteApp.vue'
import {router} from './routes'

const partialApp = createApp(Partials);
const partialEl = document.getElementById('partials');
if (partialEl) {
    partialApp.provide('noteid', partialEl.dataset.noteid)
    partialApp.provide('linkid', partialEl.dataset.linkid)
    partialApp.mount('#partials')
}

const app = createApp(NoteApp)
app.use(router)
app.mount('#partials-noteshow')
