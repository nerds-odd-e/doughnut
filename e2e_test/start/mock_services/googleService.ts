import ServiceMocker from '../../support/ServiceMocker'

const GOOGLE_PORT = 5003

const googleService = () => {
  const serviceMocker = new ServiceMocker('google', GOOGLE_PORT)

  return {
    serviceUrl: `http://localhost:${GOOGLE_PORT}`,

    async mock() {
      await serviceMocker.install()
    },

    async stubTokenExchange(accessToken: string, refreshToken: string) {
      return serviceMocker.stubPoster('/token', {
        access_token: accessToken,
        refresh_token: refreshToken,
        expires_in: 3600,
      })
    },

    async stubGmailProfile(email: string) {
      return serviceMocker.stubGetterPathOnly('/gmail/v1/users/me/profile', {
        emailAddress: email,
      })
    },
  }
}

export default googleService
