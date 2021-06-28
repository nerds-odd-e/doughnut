import { routes } from '@/routes/routes'
import {createMemoryHistory, createRouter} from 'vue-router'

  
const createTestRouter = async (currentRoute) => {
    const route = createRouter({
        history: createMemoryHistory(),
        routes,
    })
    if (!!currentRoute) {
        route.replace(currentRoute)
        await route.isReady()
    }
    return route;
}

export {createTestRouter}