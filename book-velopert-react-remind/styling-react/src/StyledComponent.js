import styled, {css} from 'styled-components';

const Box = styled.div`
  /* props로 넣어 준 값을 직접 전달해 줄 수 있습니다. */
  background: ${props => props.color || 'blue'};
  padding: 1rem;
  display: flex;
`;

const Button = styled.button`
  background: white;
  color: black;
  border-radius: 4px;
  padding: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  font-size: 1rem;
  font-weight: 600;
  
  /* & 문자를 사용하여 Sass 처럼 자기 자신을 선택 가능 */
  &:hover {
    background: rgba(255, 255, 255, 0);
  }
  
  /* 다음 코드는 inverted 값이 true 일 때 특정 스타일을 부여합니다. */
  ${props => 
    props.inverted &&
    css`
      background: none;
      border: 2px solid white;
      color: white;
      $:hover {
        background: white;
        color: black;
      }
    `};
    $ + button {
      margin-left: 1rem;
    }
`;

const StyledComponent = () => (
  <Box color="black">
    <Button>안녕하세요</Button>
    <Button inverted={true}>테두리만</Button>
  </Box>
);

export default StyledComponent;
