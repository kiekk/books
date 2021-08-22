const express = require("express");
const jwt = require("jsonwebtoken");
const cors = require("cors");

const { verifyToken, apiLimiter } = require("./middlewares");
const { Domain, User, Post, HashTag } = require("../models");
const { hash } = require("bcrypt");

const router = express.Router();

router.use(cors());

router.post("/token", apiLimiter, async (req, res) => {
  const { clientSecret } = req.body;
  try {
  } catch (e) {
    console.error(e);
    return res.status(500).json({
      code: 500,
      message: "서버 에러",
    });
  }
});

router.get("/test", verifyToken, apiLimiter, (req, res) => {
  res.json(req.decoded);
});

router.get("/posts/my", apiLimiter, verifyToken, (req, res) => {
  Post.findAll({ where: { userId: req.decoded.id } })
    .then((posts) => {
      console.log("posts", posts);
      res.json({
        code: 200,
        payload: posts,
      });
    })
    .catch((e) => {
      console.error(e);
      return res.status(500).json({
        code: 500,
        message: "서버 에러",
      });
    });
});

router.get(
  "/posts/hashtag/:title",
  verifyToken,
  apiLimiter,
  async (req, res) => {
    try {
      const hashtag = await HashTag.findOne({
        where: { title: req.params.title },
      });
      if (!hashtag) {
        return res.status(404).json({
          code: 404,
          message: "검색 결과가 없습니다.",
        });
      }
      const posts = await hashtag.getPosts();
      return res.json({
        code: 200,
        payload: posts,
      });
    } catch (e) {
      console.error(e);
    }
  }
);

module.exports = router;
