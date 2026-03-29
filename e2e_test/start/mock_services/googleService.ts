import { Mountebank } from '@anev/ts-mountebank'
import ServiceMocker from '../../support/ServiceMocker'

const GOOGLE_PORT = 5003
const mountebank = new Mountebank()

const googleService = () => {
  const serviceMocker = new ServiceMocker('google', GOOGLE_PORT)

  return {
    serviceUrl: serviceMocker.serviceUrl,

    mock() {
      return cy.wrap(serviceMocker.install())
    },

    restore() {
      return cy.wrap(
        mountebank.deleteImposter(GOOGLE_PORT).catch(() => undefined)
      )
    },

    stubTokenExchange(accessToken: string, refreshToken: string) {
      return serviceMocker.stubPoster('/token', {
        access_token: accessToken,
        refresh_token: refreshToken,
        expires_in: 3600,
      })
    },

    stubGmailProfile(email: string) {
      return serviceMocker.stubGetterPathOnly('/gmail/v1/users/me/profile', {
        emailAddress: email,
      })
    },

    stubGmailMessages(messages: { id: string }[]) {
      return serviceMocker.stubGetterPathOnly('/gmail/v1/users/me/messages', {
        messages,
      })
    },

    stubGmailMessage(id: string, subject: string) {
      return serviceMocker.stubGetterPathOnly(
        `/gmail/v1/users/me/messages/${id}`,
        {
          id,
          payload: {
            headers: [{ name: 'Subject', value: subject }],
          },
        }
      )
    },
  }
}

export default googleService
