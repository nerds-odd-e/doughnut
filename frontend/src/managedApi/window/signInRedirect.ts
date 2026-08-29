import { HealthCheckController } from "@generated/donut-backend-api/sdk.gen"

const ACTIVE_PROFILE_MARKER = "Active Profile: "
const COMMIT_MARKER = ". Commit: "

export const pingHasProdProfile = (ping: string) => {
  const start = ping.indexOf(ACTIVE_PROFILE_MARKER)
  if (start === -1) return false
  const after = ping.slice(start + ACTIVE_PROFILE_MARKER.length)
  const commitAt = after.indexOf(COMMIT_MARKER)
  const profilesPart = commitAt === -1 ? after : after.slice(0, commitAt)
  return profilesPart.split(",").some((token) => token.trim() === "prod")
}

export const signInRedirectHref = (fromHref: string, ping: string) => {
  const path = pingHasProdProfile(ping) ? "/login/continue" : "/users/identify"
  return `${path}?from=${fromHref}`
}

export const browserLocation = {
  assign(url: string) {
    window.location.href = url
  },
}

export const healthcheckPing = async (): Promise<string> => {
  const { data, error } = await HealthCheckController.ping()
  if (error) throw error
  return data!
}
