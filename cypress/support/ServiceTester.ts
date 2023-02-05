/// <reference types="cypress" />

import ServiceMocker from "./ServiceMocker"

class ServiceTester {
  serviceMocker = new ServiceMocker(5001)
  serviceName: string

  constructor(serviceName: string) {
    this.serviceName = serviceName
  }

  get serviceUrl(): string {
    return this.serviceMocker.serviceUrl
  }

  get savedServiceUrlName() {
    return `saved${this.serviceName}Url`
  }

  install() {
    this.serviceMocker.install()
  }
}

export default ServiceTester
