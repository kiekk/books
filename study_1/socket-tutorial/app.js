// 모듈 추출
const socketIo = require("socket.io");
const express = require("express");
const http = require("http");

const app = express();

const server = http.createServer(app);
const io = socketIo.listen(server);

// 미들웨어 설정
app.use(express.static(`${__dirname}/public`));

// 웹 소켓을 설정
io.sockets.on("connection", (socket) => {
  socket.on("print", (data) => {
    console.log("Client Send Data:", data);

    // public 통신
    // io.sockets.emit("smart", data);

    // broadcast 통신
    socket.broadcast.emit("smart", data);
    // socket.emit("smart", data);
  });
});

// 서버 실행
server.listen(52273, function () {
  console.log("Server Running at http://127.0.0.1:52273");
});
