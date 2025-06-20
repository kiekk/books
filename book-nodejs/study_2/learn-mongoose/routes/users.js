var express = require("express");
var User = require("../schemas/user");
var router = express.Router();

/* GET users listing. */
router.get("/", async (req, res, next) => {
  try {
    const users = await User.find();
    res.json(users);
  } catch (e) {
    console.error(e);
    next(e);
  }
});

router.post("/", async (req, res, next) => {
  try {
    const user = await new User({
      name: req.body.name,
      age: req.body.age,
      married: req.body.married,
    });
    const result = await user.save();
    console.log(result);
    res.status(201).json(result);
  } catch (e) {
    console.error(e);
    next(e);
  }
});

module.exports = router;
