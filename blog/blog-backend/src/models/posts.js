import mongoose from 'mongoose'

const { Schema } = mongoose

const PostSchema = new Schema({
  title: String,
  body: String,
  tags: [String],
  publishedDate: {
    type: Date,
    default: Date.now, // 현재 날짜를 기본값으로 지정
  },
})

const post = mongoose.model('Post', PostSchema)
export default post
