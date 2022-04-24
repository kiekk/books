import React from 'react'
import ReactDOMServer from 'react-dom/server'
import express from 'express'
import { StaticRouter } from 'react-router-dom'
import App from "./App";

const app = express()

// 서버 사이드 렌더링을 처리할 핸들러 함수
const serverRender = (req, res, next) => {
  // 이 함수는 404 에러가 발생해야 하는 상황에 404를 발생하지 않고 서버 사이드 렌더링을 해 줍니다.

  const context = {};
  const jsx = (
    <StaticRouter location={req.url} context={context}>
      <App />
    </StaticRouter>
  );
  const root = ReactDOMServer.renderToString(jsx)
  res.send(root)
}

app.use(serverRender)

app.listen(5000, () => {
  console.log('Running on http://localhost:5000')
})