import { relativeRoute } from '@/routes/relative_routes'

describe('relative routes', () => {

  test('when in repeat, go to nested route', async () => {
    const comp = {$route: {name: 'repeat'}}
    const fullRoute = relativeRoute(comp, {name: 'noteShow', params: {noteid: 3}})
    expect(fullRoute).toEqual({name: 'repeat-noteShow', params: {noteid: 3}})
  });

  test('when in repeat and the route doesnot exist, go to the original route (for now)', async () => {
    const comp = {$route: {name: 'repeat'}}
    const fullRoute = relativeRoute(comp, {name: 'review', params: {noteid: 3}})
    expect(fullRoute).toEqual({name: 'review', params: {noteid: 3}})
  });

});
