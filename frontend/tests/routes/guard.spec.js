import { routerScopeGuard } from '@/routes/relative_routes'

describe('router guards', () => {
  var confirm;
  var guard;

  beforeEach(async () => {
    confirm = jest.fn()
    guard = routerScopeGuard("repeat", ["review"], async ()=>confirm())
  });
  test('when in repeat, go to nested noteShow', async () => {
    const next = jest.fn()
    await guard({name: 'noteShow', params: {noteId: 3}}, {name: 'repeat'}, next)
    expect(next).toHaveBeenCalledWith({name: 'repeat-noteShow', params: {noteId: 3}});
  })

  test('when in repeat, and going to already nested route', async () => {
    const next = jest.fn()
    await guard({name: 'repeat-noteShow', params: {noteId: 3}}, {name: 'repeat'}, next)
    expect(next).toHaveBeenCalledWith();
  })

  test('when in repeat, and going to a route that doesnot have nested route', async () => {
    const next = jest.fn()
    confirm.mockReturnValue(true);
    await guard({name: 'initial', params: {noteId: 3}}, {name: 'repeat'}, next)
    expect(next).toHaveBeenCalledWith();
  })

  test('when in repeat, and going to a route that doesnot have nested route and not confirm', async () => {
    const next = jest.fn()
    confirm.mockReturnValue(false);
    await guard({name: 'initial'}, {name: 'repeat'}, next)
    expect(next).toHaveBeenCalledTimes(0);
  })

  test('when allowed route is called', async () => {
    const next = jest.fn()
    confirm.mockReturnValue(false);
    await guard({name: 'review'}, {name: 'repeat'}, next)
    expect(confirm).toHaveBeenCalledTimes(0);
  })

})
