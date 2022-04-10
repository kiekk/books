import {useState} from "react";

const Say = () => {
  // 변수명을 message로 하려 했기 때문에 message, setMessage로 작성했습니다.
  // 이름은 작성하기 나름입니다.
  const [message, setMessage] = useState('');
  const onClickEnter = () => setMessage('hello');
  const onClickLeave = () => setMessage('bye');

  return (
    <div>
      <button onClick={onClickEnter}>입장</button>
      <button onClick={onClickLeave}>퇴장</button>
      <h1>{message}</h1>
    </div>
  )
}

export default Say;