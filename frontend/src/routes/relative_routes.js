import { routes } from './routes'

const routerScopeGuard = (scopeName, except, alertCallback) =>{
  const routeNames = routes.find(r=>r.name===scopeName).children.map(r=>r.name)

  return async (to, from, next) => {
    if(to.name.split("-").shift() !== scopeName) {
      const nestedName = `${scopeName}-${to.name}`
      if(routeNames.includes(nestedName)) {
        if(except.includes(from.name)) {
          await alertCallback(false)
          return
        }
        next({...to, name: nestedName})
        return
      }
    }
    next()
  }

}

export { routerScopeGuard }