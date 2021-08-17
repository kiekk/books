// 모듈 추출
const fs = require("fs");
const ejs = require("ejs");
const http = require("http");
const express = require("express");

// 생성자 함수 사용
var counter = 0;
function Product(name, image, price, count) {
  this.index = counter++;
  this.name = name;
  this.image = image;
  this.price = price;
  this.count = count;
}

// 변수 선언
var products = [
  new Product("JavaScript", "peng.jpg", 28000, 30),
  new Product("jQuery", "peng.jpg", 28000, 30),
  new Product("Node.js", "peng.jpg", 37000, 30),
  new Product("Socket.Io", "peng.jpg", 17000, 30),
  new Product("Connect", "peng.jpg", 18000, 30),
  new Product("Express", "peng.jpg", 31000, 30),
  new Product("EJS", "peng.jpg", 12000, 30),
];

// 웹 서버 생성
const app = express();
const server = http.createServer(app);

// 웹 서버 설정
app.use(express.static(__dirname + "/public"));

// 라우트 수행
app.get("/", (request, response) => {
  // HTMLPage.html 파일 읽기
  var htmlPage = fs.readFileSync("HTMLPage.html", "utf8");

  // 응답
  response.send(
    ejs.render(htmlPage, {
      products,
    })
  );
});

// 웹 서버 실행
server.listen(52273, () => {
  console.log("Server Running at http://127.0.0.1:52273");
});

// 소켓 서버 생성, 실행
const io = require("socket.io").listen(server);
io.sockets.on("connection", (socket) => {
  // 함수 선언
  function onReturn(index) {
    // 물건 개수 증가
    products[index].count++;

    // 타이머 제거
    clearTimeout(cart[index].timerID);

    // 카트에서 물건 제거
    delete cart[index];

    // count 이벤트 발생
    io.sockets.emit("count", {
      index,
      count: products[index].count,
    });
  }

  // 변수 선언
  var cart = {};

  // cart 이벤트
  socket.on("cart", (index) => {
    // 물건 개수 감소
    products[index].count--;

    // 카으테 물건 넣고 타이머 실행
    cart[index] = {};
    cart[index].index = index;
    cart[index].timerID = setTimeout(() => {
      onReturn(index);
    }, 10 * 60 * 1000);

    // count 이벤트 발생
    io.sockets.emit("count", {
      index,
      count: products[index].count,
    });
  });

  // buy 이벤트
  socket.on("buy", (index) => {
    // 타이머 제거
    clearTimeout(cart[index].timerID);

    // 카트에서 물건 제거
    delete cart[index];

    // count 이벤트 발생
    io.sockets.emit("count", {
      index,
      count: products[index].count,
    });
  });

  // return 이벤트
  socket.on("return", (index) => {
    onReturn(index);
  });
});
