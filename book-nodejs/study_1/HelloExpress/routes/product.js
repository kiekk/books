var express = require("express");
var router = express.Router();

// GET - /
router.get("/", function (request, response) {
  response.render("product/index", {
    title: "Product Page",
  });
});

// GET - /product/insert
router.get("/insert", function (request, response) {
  response.render("product/insert", {
    title: "Insert Page",
  });
});

// GET - /product/edit
router.get("/edit", function (request, response) {
  response.render("product/edit", {
    title: "Edit Page",
  });
});

module.exports = router;
