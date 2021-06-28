const prefix = (router) => {
  const currentRouteName = router.currentRoute._rawValue.name
  if(!!currentRouteName) {
    if(currentRouteName.split("-").shift() === 'repeat') return 'repeat-'
  }
  return ''
}

const nestedName = (router, name) => {
  return `${prefix(router)}${name}`
}

const relativeRoute = (router, to) =>({...to, name: nestedName(router, to.name)})
const relativeRoutePush = (router, params) => { router.push(relativeRoute(router, params)) }

export { relativeRoute, relativeRoutePush }