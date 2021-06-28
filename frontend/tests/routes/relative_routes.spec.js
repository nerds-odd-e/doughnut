import { createTestRouter } from '../testing_routes'
import { relativeRoute } from '@/routes/relative_routes'

describe('relative routes', () => {

  test('when in repeat, go to nested route', async () => {
    const testingRouter = await createTestRouter({name: 'repeat'});
    const fullRoute = relativeRoute(testingRouter, {name: 'showNote', params: {noteid: 3}})
    expect(fullRoute).toEqual({name: 'repeat-showNote', params: {noteid: 3}})
  });
});
