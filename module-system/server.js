var express = require('express')
var path = require('path')
var app = express()

app.use(express.static(path.join(__dirname, '')))

var server = app.listen(3000, () => {
  var port = server.address().port
  console.log(`${port} port server`)
})