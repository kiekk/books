import React from 'react'
import ReactDOM from 'react-dom'
import { createStore, applyMiddleware } from 'redux'
import { Provider } from 'react-redux'
import './index.css'
import App from './App'
import rootReducer, { rootSaga } from './modules'
// import loggerMiddleware from './lib/loggerMiddleware';
import { createLogger } from 'redux-logger/src'
import ReduxThunk from 'redux-thunk'
import createSagaMiddleware from 'redux-saga'

const logger = createLogger()
const sagaMiddleware = createSagaMiddleware()
const store = createStore(
  rootReducer,
  applyMiddleware(logger, ReduxThunk, sagaMiddleware),
)

sagaMiddleware.run(rootSaga)

ReactDOM.render(
  <Provider store={store}>
    <App />
  </Provider>,
  document.getElementById('root'),
)
