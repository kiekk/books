var express = require("express");
var User = require("../schemas/user");
var router = express.Router();

/* GET home page. */
router.get("/", async (req, res, next) => {
  try {
    const users = await User.find();
    res.render("mongoose", { users });
  } catch (err) {
    console.error(err);
    next(err);
  }
});

module.exports = router;
