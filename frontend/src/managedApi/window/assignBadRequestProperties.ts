const assignBadRequestProperties = (object, response) => {
  const data = response?.errors
  if (!data) return
  Object.keys(data).forEach((key) => {
    if (key.includes(".")) {
      const [namespace, subkey] = key.split(".")
      if (namespace !== undefined) {
        if (!object[namespace]) object[namespace] = {}
        object[namespace][subkey!] = data[key]
      }
    } else {
      object[key] = data[key]
    }
  })
}

export default assignBadRequestProperties
