import { createApp } from 'vue'
import DoughnutApp from './DoughnutApp.vue'
import {router} from './routes'

const app = createApp(DoughnutApp)
app.use(router)
app.mount('#partials-noteshow')
