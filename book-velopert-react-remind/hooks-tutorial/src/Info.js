import {useReducer} from "react";

function reducer(state, action) {
  return {
    ...state,
    [action.name]: action.value
  }
}

export function useInputs(initialForm) {
  const [state, dispatch] = useReducer(reducer, initialForm);
  const onChange = e => {
    dispatch(e.target);
  }
  return [state, onChange];
}

const Info = () => {
  const [state, onChange] = useInputs({
    name: '',
    nickname: ''
  })
  const {name, nickname} = state;

  return (
    <div>
      <div>
        <input type="text" value={name} onChange={onChange}/>
        <input type="text" value={nickname} onChange={onChange}/>
      </div>
      <div>
        <div>
          <b>이름 :</b> {name}
        </div>
        <div>
          <b>닉네임:</b> {nickname}
        </div>
      </div>
    </div>
  )
}

export default Info;