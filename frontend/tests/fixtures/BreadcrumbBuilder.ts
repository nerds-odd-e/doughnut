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

  inCircle(value: string): BreadcrumbBuilder {
    this.data.owns = true;
    this.data.notebook = {
      ownership: {
        isFromCircle: true,
        circle: {
          name: value,
        },
      },
    };
    return this;
  }


  do(): any {
    return this.data
  }
}

export default BreadcrumbBuilder;
