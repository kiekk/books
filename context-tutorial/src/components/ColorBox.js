import ColorContext from "../contexts/color";

const ColorBox = () => {
  return (
    <ColorContext.Consumer>
      {/*
      Consumer 사이에 함수를 넣어 주었는데,
      이러한 방법을 Function as a child, Render Props 라고 합니다.
      컴포넌트의 children 이 있어야 할 자리에 일반 JSX/문자열이 아닌 함수를 전달합니다.
      */}
      {value => (
        <div
          style={{
            width: '64px',
            height: '64px',
            background: value.color
          }}
        />
      )}
    </ColorContext.Consumer>
  )
}

export default ColorBox;