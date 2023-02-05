import { Stub } from "@anev/ts-mountebank"
/// <reference types="cypress" />

import { DefaultStub, HttpMethod, FlexiPredicate } from "@anev/ts-mountebank"
import MountebankWrapper from "./MountebankWrapper"

class ServiceMocker {
  private readonly mountebank
  readonly serviceName: string

  constructor(serviceName: string, port: number) {
    this.mountebank = new MountebankWrapper(port)
    this.serviceName = serviceName
  }

  get savedServiceUrlName() {
    return `saved${this.serviceName}Url`
  }

  install() {
    this.mountebank.createImposter()
  }

  get serviceUrl() {
    return this.mountebank.serviceUrl
  }

  public stubByUrl(url: string, data: unknown) {
    return this.stub(new DefaultStub(url, HttpMethod.GET, data, 200))
  }

  public stubGetter(path: string, queryData: unknown, data: unknown) {
    return this.stub(
      new DefaultStub(path, HttpMethod.GET, data, 200).withPredicate(
        new FlexiPredicate().withPath(path).withQuery(queryData).withMethod(HttpMethod.GET),
      ),
    )
  }

  private async stub(stub: Stub) {
    this.mountebank.addStubToImposter(stub)
  }
}

export default ServiceMocker
