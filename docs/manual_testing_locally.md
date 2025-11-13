# Manual Testing

## Quick Start

Start the complete development environment (recommended):

```bash
pnpm sut
```

This starts backend, frontend, and mountebank - all with auto-reload.

To use the AI services, you will need to set the environment variable `OPENAI_API_TOKEN` before running the command.

Visit http://localhost:5173/ to see the frontend web-app with hot-reload. The backend API is available at http://localhost:9081.

Local test accounts:

* User: 'old_learner', Password: 'password'
* User: 'another_old_learner', Password: 'password'
* User: 'admin', Password: 'password'
