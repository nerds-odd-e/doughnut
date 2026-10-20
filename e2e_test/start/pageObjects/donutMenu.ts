import router from '../router'
import {
  donutMenuLauncher,
  openDonutMenu,
  pageBar,
  waitUntilAppIsNotBusy,
} from '../pageBase'
import { form } from '../forms'
import { navigateToNotebooksPage } from './notebooksPage'
import { assumeAdminDashboardPage } from './adminPages/adminDashboardPage'

const userSettingsForm = () => {
  const dailyProbeField = () => form.getField('Daily probe')
  return {
    submit() {
      cy.findByText('Submit').click()
      waitUntilAppIsNotBusy()
      return this
    },
    turnDailyProbeOn() {
      dailyProbeField().check()
      return this.submit()
    },
    turnDailyProbeOff() {
      dailyProbeField().uncheck()
      return this.submit()
    },
    reload() {
      cy.reload()
      waitUntilAppIsNotBusy()
      return this
    },
    expectDailyProbeOn() {
      dailyProbeField().shouldBeChecked()
      return this
    },
    expectDailyProbeOff() {
      dailyProbeField().shouldNotBeChecked()
      return this
    },
  }
}

export const assumeUserSettingsPage = () => userSettingsForm()

/** Opens the Donut menu panel in the app, staying on the current page. */
export const donutMenuPanel = () => {
  router().openApp()
  openDonutMenu()
  return {
    expectFullWidthSheetBelowPageBar() {
      pageBar().then(($bar) => {
        const barBottom = $bar[0]!.getBoundingClientRect().bottom
        cy.window().then((win) => {
          openDonutMenu().should(($panel) => {
            const panel = $panel[0]!.getBoundingClientRect()
            expect(panel.top, 'panel top').to.be.at.least(barBottom - 1)
            expect(panel.left, 'panel left').to.be.at.most(1)
            expect(panel.right, 'panel right').to.be.at.least(
              win.innerWidth - 1
            )
          })
        })
      })
      return this
    },
    expectAnchoredBelowLauncher() {
      donutMenuLauncher().then(($launcher) => {
        const launcher = $launcher[0]!.getBoundingClientRect()
        cy.window().then((win) => {
          openDonutMenu().should(($panel) => {
            const panel = $panel[0]!.getBoundingClientRect()
            expect(panel.top, 'panel top').to.be.at.least(launcher.bottom - 1)
            expect(
              Math.abs(panel.left - launcher.left),
              'panel left from launcher left'
            ).to.be.at.most(4)
            expect(panel.width, 'panel width').to.be.lessThan(win.innerWidth)
          })
        })
      })
      return this
    },
  }
}

export const donutMenu = () => {
  navigateToNotebooksPage()

  return {
    adminDashboard() {
      return this.userOptions().adminDashboard()
    },
    userOptions() {
      openDonutMenu().findByRole('button', { name: 'Account' }).click()
      return {
        adminDashboard() {
          cy.findByText('Admin Dashboard').click({ force: true })
          return assumeAdminDashboardPage()
        },

        userSettingsButton(userName: string) {
          return cy.findByRole('link', { name: `Settings for ${userName}` })
        },

        userSettings(userName: string) {
          this.userSettingsButton(userName).click()
          waitUntilAppIsNotBusy()
          return {
            changeName(name: string) {
              form.getField('Name').assignValue(name)
              return this.submit()
            },
            ...userSettingsForm(),
          }
        },
      }
    },
  }
}
