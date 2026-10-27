import { runOwnedE2eInvocation } from './e2e-owned-invocation.mjs'
import { runCypressOnce } from './e2e-cypress-process.mjs'
import { matchesGlob } from 'node:path'
import { startHostedAssistantFixture } from '../e2e_test/support/hosted_assistant_fixture.mjs'
import {
  ASSISTANT_LINKING_SPEC,
  HOSTED_ASSISTANT_CONTROL,
  HOSTED_ASSISTANT_BROWSER_PINS,
  HOSTED_ASSISTANT_BROWSER_ORIGIN,
} from '../e2e_test/support/hosted_assistant_constants.mjs'

/** Allocate confidential client and exact callback before the backend bootstraps clients. */
export async function runWithHostedAssistant(options) {
  if (!options.specs.some((spec) => matchesGlob(ASSISTANT_LINKING_SPEC, spec)))
    return runOwnedE2eInvocation(options)
  const fixture = await startHostedAssistantFixture()
  try {
    const sutEnv = options.sutEnv ?? options.env
    const configuration = JSON.parse(sutEnv.SPRING_APPLICATION_JSON ?? '{}')
    const spring = configuration.spring ?? {}
    const security = spring.security ?? {}
    const oauth2 = security.oauth2 ?? {}
    const donut = configuration.donut ?? {}
    const configured = {
      ...configuration,
      spring: {
        ...spring,
        security: {
          ...security,
          oauth2: {
            ...oauth2,
            client: fixture.springConfiguration.spring.security.oauth2.client,
          },
        },
      },
      donut: {
        ...donut,
        oauth: { ...donut.oauth, ...fixture.springConfiguration.donut.oauth },
      },
    }
    const selectedSession = options.session ?? runCypressOnce
    return await runOwnedE2eInvocation({
      ...options,
      https: true,
      browser: options.browser ?? 'chrome',
      sutEnv: {
        ...sutEnv,
        SPRING_APPLICATION_JSON: JSON.stringify(configured),
      },
      env: {
        ...options.env,
        [HOSTED_ASSISTANT_CONTROL]: JSON.stringify(fixture.control),
      },
      session: async (sessionOptions) => {
        const pins = fixture.configure(sessionOptions.tlsProxy)
        sessionOptions.env[HOSTED_ASSISTANT_BROWSER_PINS] = pins
        sessionOptions.env[HOSTED_ASSISTANT_BROWSER_ORIGIN] =
          sessionOptions.tlsProxy.httpsOrigin
        sessionOptions.env.ELECTRON_EXTRA_LAUNCH_ARGS = [
          sessionOptions.env.ELECTRON_EXTRA_LAUNCH_ARGS,
          `--ignore-certificate-errors-spki-list=${pins}`,
        ]
          .filter(Boolean)
          .join(' ')
        return selectedSession(sessionOptions)
      },
    })
  } finally {
    await fixture.close()
  }
}
