import React from 'react'
import ReactDOM from 'react-dom'
import './index.css'
import App from './App'
import { BrowserRouter } from 'react-router-dom'
import { createStore, applyMiddleware } from 'redux'
import { Provider } from 'react-redux'
import thunk from 'redux-thunk'
import rootReducer, { rootSaga } from './modules'
import createSagaMiddleware from 'redux-saga'
import { loadableReady } from '@loadable/component'

const sagaMiddleware = createSagaMiddleware()

const store = createStore(
  rootReducer,
  window.__PRELOADED_STATE__, // 이 값을 초기 상태로 사용
  applyMiddleware(thunk, sagaMiddleware),
)

sagaMiddleware.run(rootSaga)

const Root = () => {
  return (
    <Provider store={store}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </Provider>
  )
}

const root = document.getElementById('root')

if (process.env.NODE_ENV === 'production') {
  loadableReady(() => {
    ReactDOM.hydrate(<Root />, root)
  })
} else {
  ReactDOM.render(<Root />, root)
}
