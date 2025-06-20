// 모듈 추출
const socketIo = require("socket.io");
const express = require("express");
const http = require("http");
const fs = require("fs");

// 변수 선언
var seats = [
  [1, 1, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 1, 1],
  [1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1],
  [1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1],
  [1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1],
  [1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1],
  [1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1],
  [1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1],
  [1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1],
  [1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1],
  [1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1],
  [1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1],
  [1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1],
];

// 웹 서버 생성
const app = express();
const server = http.createServer(app);

// 라우트 수행
app.get("/", (request, response, next) => {
  fs.readFile("HTMLPage.html", (error, data) => {
    response.send(data.toString());
  });
});

app.get("/seats", (request, response, next) => {
  response.send(seats);
});

// 웹 서버 실행
server.listen(52273, () => {
  console.log("Server Running at http://127.0.0.1:52273");
});

// 소켓 서버 생성, 실행
const io = socketIo.listen(server);
io.sockets.on("connection", (socket) => {
  socket.on("reserve", (data) => {
    seats[data.y][data.x] = 2;
    io.sockets.emit("reserve", data);
  });
});
