import { routes } from './routes'

const routeNames = (routes) => {
  return routes.map(r=>r.name).concat(routes.map(r=>r.children).filter(c=>c).map(c=>routeNames(c)).flat())
}

const prefix = (route) => {
  const currentRouteName = route.name
  if(!!currentRouteName) {
    if(currentRouteName.split("-").shift() === 'repeat') return 'repeat-'
  }
  return ''
}

const nestedName = (route, name) => {
  return `${prefix(route)}${name}`
}

const relativeRoute = (comp, to) =>{
  const nested = nestedName(comp.$route, to.name)
  if(routeNames(routes).includes(nested)) {
    return {...to, name: nested}
  }
  return to
}

const relativeRoutePush = (comp, params) => { comp.$router.push(relativeRoute(comp, params)) }

export { relativeRoute, relativeRoutePush }