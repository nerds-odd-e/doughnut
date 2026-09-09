const COMPOSED_COMPLETION_EVENTS = new Set(['after:spec', 'after:run'])

/**
 * Cypress keeps one server callback per event. Wrap `on` so every
 * after:spec / after:run handler still runs and returned promises settle.
 */
export function composeCypressPluginEvents(on) {
  const handlersByEvent = new Map()

  return (event, handler) => {
    if (!COMPOSED_COMPLETION_EVENTS.has(event)) {
      return on(event, handler)
    }
    let handlers = handlersByEvent.get(event)
    if (!handlers) {
      handlers = []
      handlersByEvent.set(event, handlers)
      on(event, async (...args) => {
        const errors = []
        for (const fn of handlers) {
          try {
            await fn(...args)
          } catch (error) {
            errors.push(error)
          }
        }
        if (errors.length > 0) throw errors[0]
      })
    }
    handlers.push(handler)
  }
}
