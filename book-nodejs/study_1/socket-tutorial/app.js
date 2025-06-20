// 모듈 추출
const socketIo = require("socket.io");
const express = require("express");
const http = require("http");

const app = express();

const server = http.createServer(app);
const io = socketIo.listen(server);

// 미들웨어 설정
app.use(express.static(`${__dirname}/public`));

var id = 0;
// 웹 소켓을 설정
io.sockets.on("connection", (socket) => {
  // id 설정
  id = socket.id;

  socket.on("print", (data) => {
    console.log("Client Send Data:", data);
    // private 통신
    io.sockets.to(id).emit("smart", data);
  });
});

// 서버 실행
server.listen(52273, function () {
  console.log("Server Running at http://127.0.0.1:52273");
});
