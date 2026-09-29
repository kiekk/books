# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a standard Create React App project bootstrapped with `create-react-app`. It uses React 19.2.0 and react-scripts 5.0.1 for build tooling.

## Development Commands

### Running the Development Server
```bash
npm start
```
Starts the development server on http://localhost:3000 with hot reloading enabled.

### Running Tests
```bash
# Run all tests in watch mode
npm test

# Run a specific test file
npm test -- App.test.js

# Run tests without watch mode (CI environment)
CI=true npm test
```

Tests use Jest and React Testing Library (@testing-library/react). The testing setup is configured in `src/setupTests.js` which imports @testing-library/jest-dom for custom matchers.

### Building for Production
```bash
npm run build
```
Creates an optimized production build in the `build/` folder with minified files and hashed filenames.

## Architecture

### Application Structure
- **Entry Point**: `src/index.js` - Creates React root and renders the App component wrapped in React.StrictMode
- **Main Component**: `src/App.js` - Root application component
- **Static Assets**: `public/` directory contains index.html template and static assets (favicon, logos, manifest)

### Testing Setup
- Jest is the test runner (configured via react-scripts)
- React Testing Library is used for component testing
- `src/setupTests.js` imports jest-dom for enhanced DOM assertions
- Test files follow the `*.test.js` naming convention

### Build Configuration
Build configuration is managed by react-scripts and includes:
- Webpack bundling
- Babel transpilation
- ESLint (extends react-app and react-app/jest configs)
- Browserslist configuration for production and development targets
