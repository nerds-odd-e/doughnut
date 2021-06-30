import { routes } from './routes'

const routerProtectGuard = (except, message) =>{
  return (to, from, next) => {
    alert(to.name)
    if(!except.includes(to.name)) {
      if(!confirm(message)) return
    }
    next()
  }
}

const routerScopeGuard = (scopeName, except, message) =>{
  const routeNames = routes.find(r=>r.name===scopeName).children.map(r=>r.name)
  const protectGuard =  routerProtectGuard(except, message)

  return (to, from, next) => {
    if(to.name.split("-").shift() !== scopeName) {
      const nestedName = `${scopeName}-${to.name}`
      if(routeNames.includes(nestedName)) {
        next({...to, name: nestedName})
        return
      }
      return protectGuard(to, from, next)
    }
    next()
  }
}

export { routerProtectGuard, routerScopeGuard }