var express = require("express");
var Comment = require("../schemas/comment");

var router = express.Router();
router.get("/:id", async (req, res, next) => {
  try {
    const comments = await Comment.find({ commenter: req.params.id }).populate(
      "commenter"
    );
    res.json(comments);
  } catch (e) {
    console.error(e);
    next(e);
  }
});

router.post("/", async (req, res, next) => {
  try {
    const comment = await new Comment({
      commenter: req.body.id,
      comment: req.body.comment,
    });
    let result = await comment.save();
    result = await Comment.populate(result, { path: "commenter" });
    res.status(201).json(result);
  } catch (e) {
    console.error(e);
    next(e);
  }
});

router.patch("/:id", async (req, res, next) => {
  try {
    const result = await Comment.update(
      { _id: req.params.id },
      { comment: req.body.comment }
    );
    res.json(result);
  } catch (e) {
    console.error(e);
    next(e);
  }
});

router.delete("/:id", async (req, res, next) => {
  try {
    const result = await Comment.remove({ _id: req.params.id });
    res.json(result);
  } catch (e) {
    console.error(e);
    next(e);
  }
});

module.exports = router;
