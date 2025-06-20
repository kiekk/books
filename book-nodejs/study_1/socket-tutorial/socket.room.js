// 모듈 추출
const socketIo = require("socket.io");
const express = require("express");
const http = require("http");

const app = express();

const server = http.createServer(app);
const io = socketIo.listen(server);

// 미들웨어 설정
app.use(express.static(`${__dirname}/public`));

// 소켓 서버 이벤트 연결
io.sockets.on("connection", (socket) => {
  // 방 이름 저장할 변수
  var roomName = null;

  // join 이벤트
  socket.on("join", (data) => {
    roomName = data;
    socket.join(data);
  });

  // messgae 이벤트
  socket.on("message", (data) => {
    io.sockets.in(roomName).emit("message", "test");
  });
});

// 서버 실행
server.listen(52273, function () {
  console.log("Server Running at http://127.0.0.1:52273");
});
