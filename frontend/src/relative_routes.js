const prefix = (router) => {
  const currentRouteName = router.currentRoute._rawValue.name
  if(!!currentRouteName) {
    if(currentRouteName.split("-").shift() === 'repeat') return 'repeat-'
  }
  return ''
}

const relativeRoute = (router, to) =>({...to, name: `${prefix(router)}${to.name}`})

export {relativeRoute}