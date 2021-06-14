import { createApp } from 'vue'
import Partials from './Partials.vue'
import TempApp from './TempApp.vue'

const partialApp = createApp(Partials);
const partialEl = document.getElementById('partials');
if (partialEl) {
    partialApp.provide('noteid', partialEl.dataset.noteid)
    partialApp.provide('linkid', partialEl.dataset.linkid)
    partialApp.mount('#partials')
}

const app = createApp(TempApp);
const el = document.getElementById('partials-noteshow');
if (el) {
    app.provide('noteid', el.dataset.noteid)
    app.mount('#partials-noteshow')
}