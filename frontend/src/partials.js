import { createApp } from 'vue';
import Partials from './Partials.vue';
import './index.css';

const app = createApp(Partials);

window.installVueComp = (locator) => {
    app.mount(locator);
};
