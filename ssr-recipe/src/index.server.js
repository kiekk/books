import React from 'react'
import ReactDOMServer from 'react-dom/server'
import express from 'express'
import { StaticRouter } from 'react-router-dom'
import App from './App'
import path from 'path'
import fs from 'fs'
import { createStore, applyMiddleware } from 'redux'
import { Provider } from 'react-redux'
import thunk from 'redux-thunk'
import rootReducer from './modules'
import PreloadContext from './lib/PreloadContext'

// asset-manifest.json 에서 파일 경로 조회
const manifest = JSON.parse(
  fs.readFileSync(path.resolve('./build/asset-manifest.json'), 'utf-8'),
)

const chunks = Object.keys(manifest.files)
  .filter((key) => /chunk\.js$/.exec(key)) // chunk.js로 끝나는 키를 찾음
  .map((key) => `<script src="${manifest.files[key]}"></script>`) // 스크립트 태그로 변환
  .join('') // 합침

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
            <script src="${manifest.files['runtime-main.js']}"></script>
            ${chunks}
            <script src="${manifest.files['main.js']}"></script>
          </body>
        </html>
    `
}

const app = express()

// 서버 사이드 렌더링을 처리할 핸들러 함수
const serverRender = async (req, res, next) => {
  // 이 함수는 404가 떠야 하는 상황에 404를 띄우지 않고 서버 사이드 렌더링을 함

  const context = {}
  const store = createStore(rootReducer, applyMiddleware(thunk))
  const preloadContext = {
    done: false,
    promises: [],
  }

  const jsx = (
    <PreloadContext.Provider value={preloadContext}>
      <Provider store={store}>
        <StaticRouter location={req.url} context={context}>
          <App />
        </StaticRouter>
      </Provider>
    </PreloadContext.Provider>
  )

  ReactDOMServer.renderToStaticMarkup(jsx) // renderToStaticMarkup으로 한번 렌더링
  try {
    await Promise.all(preloadContext.promises) // 모든 프로미스를 기다림
  } catch (e) {
    return res.status(500)
  }
  preloadContext.done = true

  const root = ReactDOMServer.renderToString(jsx) // 렌더링

  // JSON을 문자열로 변환하고 악성 스크립트가 실행되는 것을 방지하기 위해 <를 치환
  const stateString = JSON.stringify(store.getState()).replace(/</g, '\\u003c')
  const stateScript = `<script>__PRELOADED_STATE__ = ${stateString}</script>`
  res.send(createPage(root, stateScript)) // 클라이언트에게 응답
}

const serve = express.static(path.resolve('./build'), {
  index: false, // "/" 경로에서 index.html 보여주지 않도록 설정
})

app.use(serve) // serverRender 전에 위치해야함. 순서 중요
app.use(serverRender)

// 5000 포트로 서버 가동
app.listen(5000, () => {
  console.log('Running on http://localhost:5000')
})
