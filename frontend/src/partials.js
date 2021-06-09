import { createApp } from 'vue'
import Partials from './Partials.vue'

const app = createApp(Partials);
app.provide('noteid', document.getElementById('partials').dataset.noteid)
app.mount('#partials')