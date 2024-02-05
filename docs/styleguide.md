# Style Guide

- For typescript code, turn on autofix on save in VSCode IDE:
  - Install these following VSCode extensions/plugins:
    1. [Vue.volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar)
    2. [esbenp.prettier-vscode](https://marketplace.visualstudio.com/items?itemName=esbenp.prettier-vscode)
    3. [dbaeumer.vscode-eslint](https://marketplace.visualstudio.com/items?itemName=dbaeumer.vscode-eslint)

```json
"editor.codeActionsOnSave": {
  "source.organizeImports": "explicit",
  "source.fixAll": "explicit"
}
```

- Use [EditorConfig](https://editorconfig.org/) for general coding styles.
  - Most Editors / IDEs support EditorConfig already.  
    [Download a plugin](https://editorconfig.org/#download) if your tool doesn't support it
    natively.
- Use [Spotless](https://github.com/diffplug/spotless)
  with [google-java-format](https://github.com/google/google-java-format) for Java source code.
  - Follow [Using the formatter](https://github.com/google/google-java-format#using-the-formatter)
    to set up formatter or plugin for your IDE.
    - :warning: For IntelliJ, download
      the [IntelliJ Java Google Style file](https://raw.githubusercontent.com/google/styleguide/gh-pages/intellij-java-google-style.xml)
      and import it into File→Settings→Editor→Code Style.  
      ![Google Java Style](images/import_google_java_style.png)
