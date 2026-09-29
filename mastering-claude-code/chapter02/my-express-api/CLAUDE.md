# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Node.js project using Express.js v5.1.0 for building a REST API. The project is currently in its initial setup phase with Express installed but minimal application code.

## Development Commands

```bash
# Install dependencies
npm install

# Run the application (once implemented)
node app.js
```

## Architecture

The project follows a standard Express.js application structure:

- **app.js**: Main application entry point (currently empty, needs implementation)
- **package.json**: Project dependencies and npm scripts
- **Node.js version**: Uses Express 5.1.0 (latest major version with breaking changes from v4)

## Express 5.x Considerations

This project uses Express 5.x which has significant changes from 4.x:

- Route parameters and query strings are now parsed using the native `URL` and `URLSearchParams` APIs
- Middleware and routing changes - some Express 4 patterns may not work
- Promises are now supported natively in route handlers
- Error handling has been updated to support async/await
