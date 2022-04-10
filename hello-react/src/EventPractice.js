import {useState} from "react";

const EventPractice = () => {
  const [form, setForm] = useState({
    username: '',
    message: ''
  })
  const { username, message } = form;
  const onChange = e => {
    const nextForm = {
      ...form,  // 기존의 form 내용을 복사
      [e.target.name]: e.target.value // 원하는 값을 덮어 씌우기
    }
    setForm(nextForm);
  }
  const onClick = () => {
    setForm({
      username: '',
      message: ''
    })
  }
  const onKeyPress = e => {
    if (e.key === 'Enter') {
      onClick();
    }
  }

  return (
    <div>
      <h1>이벤트 연습</h1>
      <input type="text"
      name="username"
      value={username}
      onChange={onChange}/>
      <input type="text"
      name="message"
      value={message}
      onChange={onChange}
      onKeyPress={onKeyPress}/>
      <button onClick={onClick}>Reset</button>
      <h2>{username}: {message}</h2>
    </div>
  )
}

export default EventPractice;