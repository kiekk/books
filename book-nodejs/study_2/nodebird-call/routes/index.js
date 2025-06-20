const express = require("express");
const axios = require("axios");

const router = express.Router();
const URL = "http://localhost:8002/v2";

axios.defaults.headers.origin = "http://localhost:8003";

const request = async (req, api) => {
  try {
    if (!req.session.jwt) {
      // 세션에 토큰이 없으면
      const tokenResult = await axios.post(`${URL}/token`, {
        clientSecret: process.env.CLIENT_SECRET,
      });
      // 세션에 토큰 저장
      req.session.jwt = tokenResult.data.token;
    }
    return await axios.get(`${URL}${api}`, {
      headers: { authorization: req.session.jwt },
    }); // API 요청
  } catch (e) {
    console.error(e);
    if (e.response.status < 500) {
      return e.response;
    }
    throw e;
  }
};

router.get("/mypost", async (req, res, next) => {
  try {
    const result = await request(req, "/posts/my");
    res.json(result.data);
  } catch (e) {
    console.error(e);
    next(e);
  }
});

router.get("/search/:hashtag", async (req, res, next) => {
  try {
    console.log("params", req.params.hashtag);
    const result = await request(
      req,
      `/posts/hashtag/${encodeURIComponent(req.params.hashtag)}`
    );
    res.json(result.data);
  } catch (e) {
    console.error(e);
    next(e);
  }
});

router.get("/test", async (req, res, next) => {
  try {
    if (!req.session.jwt) {
      // 세션에 토큰이 없으면
      const tokenResult = await axios.post("http://localhost:8002/v1/token", {
        clientSecret: process.env.CLIENT_SECRET,
      });
      if (tokenResult.data && tokenResult.data.code === 200) {
        // 토큰 발급 성공
        // 세션에 토큰 저장
        req.session.jwt = tokenResult.data.token;
      } else {
        // 토큰 발급 실패
        // 발급 실패 사유 응답
        return res.json(tokenResult.data);
      }
    }
    //발급 받은 토큰 테스트
    const result = await axios.get("http://localhost:8002/v1/test", {
      headers: { authorization: req.session.jwt },
    });
    return res.json(result.data);
  } catch (e) {
    console.error(e);
    if (e.response.status === 419) {
      // 토큰 만료 시
      return res.json(e.response.data);
    }
    return next(e);
  }
});

router.get("/", (req, res) => {
  res.render("main", { key: process.env.CLIENT_SECRET });
});

module.exports = router;
