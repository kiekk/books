module.exports = (sequelize, DataTypes) => {
  return sequelize.define(
    "post",
    {
      content: {
        type: DataTypes.STRING(140),
        allowNull: true,
      },
      img: {
        type: DataTypes.STRING(200),
        allowNull: true,
      },
    },
    {
      timestamps: true,
      paranoid: true,
    }
    // timestamps, paranoid가 true일 경우
    // createdAt, updatedAt, deletedAt 컬럼이 자동 생성
  );
};
