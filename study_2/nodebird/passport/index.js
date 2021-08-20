const local = require("passport-local");
const kakao = require("passport-kakao");
const { User } = require("../models");

module.exports = (passport) => {
  passport.serializeUser((user, done) => {
    done(null, user.id);
  });

  passport.deserializeUser(async (id, done) => {
    try {
      const user = await User.findOne({ where: { id } });
      done(null, user);
    } catch (e) {
      done(e);
    }
  });

  local(passport);
  kakao(passport);
};
