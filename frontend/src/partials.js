import { createApp } from 'vue'
import NoteApp from './NoteApp.vue'
import {router} from './routes'

const app = createApp(NoteApp)
app.use(router)
app.mount('#partials-noteshow')
