// 모듈 추출
const http = require("http");
const fs = require("fs");
const socketIo = require("socket.io");

// 웹 서버 생성
const server = http
  .createServer((request, response) => {
    // HTMLPage.html 파일 읽기
    fs.readFile("HTMLPage.html", (error, data) => {
      response.writeHead(200, { "Content-Type": "text/html" });
      response.end(data);
    });
  })
  .listen(52273, () => {
    console.log("Server Running at http://127.0.0.1:52273");
  });

// 소켓 서버 생성
const io = socketIo.listen(server);
io.sockets.on("connection", (socket) => {
  // message 이벤트
  socket.on("message", (data) => {
    // 클라이언트의 message 이벤트 발생
    io.sockets.emit("message", data);
  });
});
