// 모듈 추출
const fs = require('fs')
const ejs = require('ejs')
const mysql = require('mysql')
const express = require('express')
const bodyParser = require('body-parser')

// 데이터베이스와 연결
const client = mysql.createConnection({
  user: 'root',
  password: '비밀번호', // root 계정에 설정했던 비밀번호 입력
  database: 'Company'
})

// 서버 생성
const app = express()
app.use(bodyParser.urlencoded({
  extended: false
}))

// 서버 실행
app.listen(52273, function() {
  console.log('server running at http://127.0.0.1:52273')
})
