import {useState} from "react";

const EventPractice = () => {
  const [username, setUsername] = useState('');
  const [message, setMessage] = useState('');
  const onChangeUsername = e => setUsername(e.target.value);
  const onChangeMessage = e => setMessage(e.target.value);
  const onClick = () => {
    setUsername('');
    setMessage('');
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
      onChange={onChangeUsername}/>
      <input type="text"
      name="message"
      value={message}
      onChange={onChangeMessage}
      onKeyPress={onKeyPress}/>
      <button onClick={onClick}>Reset</button>
      <h2>{username}: {message}</h2>
    </div>
  )
}

export default EventPractice;