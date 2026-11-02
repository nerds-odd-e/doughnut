import { devNull } from 'node:os'
import { createRequire } from 'node:module'
import { createCucumberProofRecorder } from './cucumberProofRecorder.mjs'

const require = createRequire(import.meta.url)
const {
  addCucumberPreprocessorPlugin,
  resolvePreprocessorConfiguration,
} = require('@badeball/cypress-cucumber-preprocessor')

/** Preserve active reports; otherwise consume only the message transport. */
export async function addCucumberPreprocessorWithProof(on, config, repoRoot) {
  const recorder = createCucumberProofRecorder({
    repoRoot,
    projectRoot: config.projectRoot,
  })
  if (!(config.isTextTerminal && recorder.configured)) {
    return addCucumberPreprocessorPlugin(on, config)
  }
  const resolved = await resolvePreprocessorConfiguration(
    config,
    config.expose,
    '/'
  )
  if (resolved.dryRun) return addCucumberPreprocessorPlugin(on, config)
  const messageOnly = !resolved.isTrackingState
  if (messageOnly) {
    config.expose.messagesEnabled = true
    config.expose.messagesOutput = devNull
  }
  const bridge = (event, handler) => {
    if (event === 'before:run') {
      return on(event, (...args) => {
        recorder.start()
        return messageOnly ? undefined : handler(...args)
      })
    }
    if (event !== 'task') return on(event, handler)
    const tasks = Object.fromEntries(
      Object.entries(handler).map(([name, original]) => [
        name,
        name.startsWith('cypress-cucumber-preprocessor:')
          ? async (...args) => {
              const result = messageOnly ? true : await original(...args)
              recorder.observe(name, args[0])
              return result
            }
          : original,
      ])
    )
    return on(event, tasks)
  }
  return addCucumberPreprocessorPlugin(
    bridge,
    config,
    messageOnly
      ? {
          omitAfterRunHandler: true,
          omitBeforeSpecHandler: true,
          omitAfterSpecHandler: true,
          omitAfterScreenshotHandler: true,
        }
      : {}
  )
}
