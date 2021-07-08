import { routes } from './routes'

const routerProtectGuard = (except, confirmCallback) =>{
  return async (to, from, next) => {
    if(!except.includes(to.name)) {
      if(!await confirmCallback()) return
    }
    next()
  }
}

const routerScopeGuard = (scopeName, except, confirmCallback) =>{
  const routeNames = routes.find(r=>r.name===scopeName).children.map(r=>r.name)
  const protectGuard =  routerProtectGuard(except, confirmCallback)

  return async (to, from, next) => {
    if(to.name.split("-").shift() !== scopeName) {
      const nestedName = `${scopeName}-${to.name}`
      if(routeNames.includes(nestedName)) {
        next({...to, name: nestedName})
        return
      }
      return await protectGuard(to, from, next)
    }
    next()
  }
}

export { routerProtectGuard, routerScopeGuard }