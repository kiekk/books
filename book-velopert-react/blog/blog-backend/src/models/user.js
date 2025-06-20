import mongoose, { Schema } from 'mongoose'
import bcrypt from 'bcrypt'
import jwt from 'jsonwebtoken'

const UserSchema = new Schema({
  username: String,
  hashedPassword: String,
})

UserSchema.methods.setPassword = async function (password) {
  const hash = await bcrypt.hash(password, 10)
  this.hashedPassword = hash
}

UserSchema.methods.checkPassword = async function (password) {
  const result = await bcrypt.compare(password, this.hashedPassword)
  return result // true | false
}

UserSchema.statics.findByUsername = function (username) {
  return this.findOne({ username })
}

UserSchema.methods.serialize = function () {
  const data = this.toJSON()
  delete data.hashedPassword
  return data
}

UserSchema.methods.generateToken = function () {
  const token = jwt.sign(
    // 첫 번째 파라미터에는 토큰 안에 집어넣고 싶은 데이터를 넣습니다.
    {
      _id: this.id,
      username: this.username,
    },
    process.env.JWT_SECRET, // 두 번째 파라미터에는 JWT 암호를 넣습니다.
    {
      expiresIn: '7d', // 7일 동안 유효
    },
  )
  return token
}

const User = mongoose.model('User', UserSchema)
export default User
