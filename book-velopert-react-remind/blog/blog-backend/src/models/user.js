import mongoose, {Schema} from "mongoose";
import bcrypt from 'bcrypt'
import jwt from 'jsonwebtoken'

const UserSchema = new Schema({
  username: String,
  hashedPassword: String
})

UserSchema.methods.setPassword = async function (password) {
  this.hashedPassword = await bcrypt.hash(password, 10)
}

UserSchema.methods.checkPassword = async function (password) {
  return await bcrypt.compare(password, this.hashedPassword)
}

UserSchema.statics.findByUsername = function (username) {
  return this.findOne({username})
}

UserSchema.methods.serialize = function () {
  const data = this.toJSON();
  delete data.hashedPassword;

  return data;
}

UserSchema.methods.generateToken = function () {
  return jwt.sign({
      _id: this.id,
      username: this.username
    },
    process.env.JWT_SECRET,
    {
      expiresIn: '7d'
    }
  );
}

const User = mongoose.model('User', UserSchema);

export default User;