import React from 'react'
import ReactDOMServer from 'react-dom/server'
import express from 'express'
import {StaticRouter} from 'react-router-dom'
import App from "./App";
import path from 'path'
import fs from 'fs'
import {createStore, applyMiddleware} from "redux";
import {Provider} from 'react-redux'
import thunk from "redux-thunk";
import PreloadContext from "./lib/PreloadContext";
import rootReducer, {rootSaga} from "./modules";
import createSagaMiddleware from "redux-saga";
import {END} from 'redux-saga'

const app = express()

// asset-manifest.json 에서 파일 경로들을 조회합니다.
const manifest = JSON.parse(
  fs.readFileSync(path.resolve('./build/asset-manifest.json'), 'utf-8')
)

const chunks = Object.keys(manifest.files)
  .filter(key => /chunk\.js$/.exec(key))  // chunk.js로 끝나는 키를 찾음
  .map(key => `<script src="${manifest.files[key]}"></script>`) // 스크립트 태그로 변환
  .join('')

function createPage(root, stateScript) {
  return `
   <!DOCTYPE html>
        <html lang="en">
          <head>
            <meta charset="utf-8" />
            <link rel="shortcut icon" href="/favicon.ico" />
            <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no" />
            <meta name="theme-color" content="#000000" />
            <title>React App</title>
            <link href="${manifest.files['main.css']}" rel="stylesheet" />
          </head>
          <body>
            <noscript>You need to enable JavaScript to run this app.</noscript>
            <div id="root">
              ${root}
            </div>
            ${stateScript}
            <script src="${manifest.files['runtime~main.js']}"></script>
            ${chunks}
            <script src="${manifest.files['main.js']}"></script>
          </body>
        </html>
  `
}

// 서버 사이드 렌더링을 처리할 핸들러 함수
const serverRender = async (req, res, next) => {
  // 이 함수는 404 에러가 발생해야 하는 상황에 404를 발생하지 않고 서버 사이드 렌더링을 해 줍니다.

  const context = {};
  const sagaMiddleware = createSagaMiddleware()
  const store = createStore(rootReducer, applyMiddleware(thunk, sagaMiddleware))

  const sagaPromise = sagaMiddleware.run(rootSaga).toPromise()

  const preloadContext = {
    done: false,
    promises: []
  }

  const jsx = (
    <PreloadContext.Provider value={preloadContext}>
      <Provider store={store}>
        <StaticRouter location={req.url} context={context}>
          <App/>
        </StaticRouter>
      </Provider>
    </PreloadContext.Provider>
  );

  ReactDOMServer.renderToStaticMarkup(jsx)
  store.dispatch(END)

  try {
    await sagaPromise
    await Promise.all(preloadContext.promises)
  } catch (e) {
    return res.status(500)
  }

  preloadContext.done = true

  const root = ReactDOMServer.renderToString(jsx)
  // JSON 문자열을 변환하고 악성 스크립트를 방지하기 위해 <를 치환 처리
  const stateString = JSON.stringify(store.getState()).replace(/</g, '\\u003c')
  const stateScript = `<script>__PRELOADED_STATE__ = ${stateString}</script>` // 리덕스 초기 상태를 스크립트로 주입
  res.send(createPage(root, stateScript))
}

const serve = express.static(path.resolve('./build'), {
  index: false  // "/" 경로에서 index.html을 보여주지 않도록 설정
})

app.use(serve)  // serverRender 보다 먼저 위치해야 합니다.
app.use(serverRender)

app.listen(5000, () => {
  console.log('Running on http://localhost:5000')
})