import Builder from "./Builder";


class BreadcrumbBuilder extends Builder {
  data: any;

  constructor(parentBuilder?: Builder) {
    super(parentBuilder);
    this.data = {
      owns: true,
      ancestors: [],
      notebook: {
        ownership: {
          isFromCircle: false,
        }
      }
    }
  }

  inBazaar(): BreadcrumbBuilder {
    this.data.owns = false
    return this
  }

  do(): any {
    return this.data
  }
}

export default BreadcrumbBuilder;
