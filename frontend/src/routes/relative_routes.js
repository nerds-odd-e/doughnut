import { routes } from './routes'

const relativeRoute = (comp, to) =>{
  return to
}

const relativeRoutePush = (comp, params) => { comp.$router.push(relativeRoute(comp, params)) }

const routerScopeGuard = (scopeName) =>{
  const routeNames = routes.find(r=>r.name===scopeName).children.map(r=>r.name)

  return (to, from, next) => {
    if(to.name.split("-").shift() !== scopeName) {
      if(from.name.split("-").shift() === scopeName) {
        const nestedName = `${scopeName}-${to.name}`
        if(routeNames.includes(nestedName)) {
          next({...to, name: nestedName})
          return
        }
      }
    }
    next()
  }
}

export { relativeRoute, relativeRoutePush, routerScopeGuard }