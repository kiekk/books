import Joi from '@hapi/joi'
import User from '../../models/user'

export const register = async ctx => {
  const schema = Joi.object().keys({
    username: Joi.string().alphanum().min(3).max(20).required(),
    password: Joi.string().required()
  })

  const result = schema.validate(ctx.request.body)

  if (result.error) {
    ctx.status = 400;
    ctx.body = result.error;
    return;
  }

  const {username, password} = ctx.request.body;

  try {
    const exists = await User.findByUsername(username);

    if (exists) {
      ctx.status = 409;
      return;
    }

    const user = new User({
      username,
    })
    await user.setPassword(password);
    await user.save();

    ctx.body = user.toJSON();
  } catch (e) {
    ctx.throw(500, e);
  }
}