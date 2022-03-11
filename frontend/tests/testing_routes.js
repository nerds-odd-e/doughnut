import { createMemoryHistory, createRouter } from 'vue-router';
import routes from '@/routes/routes';

const createTestRouter = async (currentRoute) => {
  const route = createRouter({
    history: createMemoryHistory(),
    routes,
  });
  if (currentRoute) {
    route.replace(currentRoute);
    await route.isReady();
  }
  return route;
};

export default createTestRouter;
