import { routerScopeGuard } from '@/routes/relative_routes'

describe('router guards', () => {
  const guard = routerScopeGuard("repeat", ["review"])

  test('when in repeat, go to nested noteShow', async () => {
    const comp = {$route: {name: 'repeat'}}
    const next = jest.fn()
    guard({name: 'noteShow', params: {noteid: 3}}, {name: 'repeat'}, next)
    expect(next).toHaveBeenCalledWith({name: 'repeat-noteShow', params: {noteid: 3}});
  })

  test('when in repeat, and going to already nested route', async () => {
    const comp = {$route: {name: 'repeat'}}
    const next = jest.fn()
    guard({name: 'repeat-noteShow', params: {noteid: 3}}, {name: 'repeat'}, next)
    expect(next).toHaveBeenCalledWith();
  })

  test('when in repeat, and going to a route that doesnot have nested route', async () => {
    const comp = {$route: {name: 'repeat'}}
    const next = jest.fn()
    window.confirm = jest.fn(() => true);
    guard({name: 'initial', params: {noteid: 3}}, {name: 'repeat'}, next)
    expect(next).toHaveBeenCalledWith();
  })

  test('when in repeat, and going to a route that doesnot have nested route and not confirm', async () => {
    const comp = {$route: {name: 'repeat'}}
    const next = jest.fn()
    window.confirm = jest.fn(() => false);
    guard({name: 'initial'}, {name: 'repeat'}, next)
    expect(next).toHaveBeenCalledTimes(0);
  })

  test('when allowed route is called', async () => {
    const comp = {$route: {name: 'repeat'}}
    const next = jest.fn()
    window.confirm = jest.fn(() => false);
    guard({name: 'review'}, {name: 'repeat'}, next)
    expect(window.confirm).toHaveBeenCalledTimes(0);
  })

})
