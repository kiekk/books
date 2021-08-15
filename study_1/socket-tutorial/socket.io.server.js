// 모듈 추출
const http = require("http");
const fs = require("fs");
const socketIo = require("socket.io");

// 서버 생성
const server = http
  .createServer(function (request, response) {
    // HTMLPage.html 파일 읽기
    fs.readFile("HTMLPage.html", function (error, data) {
      response.writeHead(200, { "Content-Type": "text/html" });
      response.send(data);
    });
  })
  .listen(52273, function () {
    console.log("Server running at http://127.0.0.1:52273");
  });
