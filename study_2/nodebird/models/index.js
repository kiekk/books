const Sequelize = require("sequelize");
const { DataTypes } = require("sequelize");
const env = process.env.NODE_ENV || "development";
const config = require("../config/config")[dev];
const db = {};

const sequelize = new Sequelize(
  config.database,
  connfig.username,
  config.password,
  config
);

db.sequelize = sequelize;
db.Sequelize = Sequelize;
db.User = require("./user")(sequelize, DataTypes);
db.Post = require("./post")(sequelize, DataTypes);
db.HashTag = require("./hashtag")(sequelize, DataTypes);

// DB 관계 설정
db.User.hasMany(db.Post);
db.Post.belongsTo(db.User);
db.Post.belongsToMany(db.HashTag, { through: "PostHashTag" });
db.Hashtag.belongsToMany(db.Post, { through: "PostHashTag" });
db.User.belongsToMany(db.User, {
  foreignKey: "followingId",
  as: "Followers",
  through: "Follow",
});
db.User.belongsToMany(db.User, {
  foreignKey: "followerId",
  as: "Followings",
  through: "Follow",
});

module.exports = db;
