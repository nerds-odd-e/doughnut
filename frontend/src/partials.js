import { createApp } from 'vue'
import Review from './Review.vue'
import NoteApp from './NoteApp.vue'
import {router} from './routes'

const partialApp = createApp(Review);
const partialEl = document.getElementById('partials');
if (partialEl) {
    partialApp.provide('noteid', partialEl.dataset.noteid)
    partialApp.provide('linkid', partialEl.dataset.linkid)
    partialApp.provide('reviewPointId', partialEl.dataset.reviewpointid)
    partialApp.provide('sadOnly', partialEl.dataset.sadonly === "true")
    partialApp.mount('#partials')
}

const app = createApp(NoteApp)
app.use(router)
app.mount('#partials-noteshow')
