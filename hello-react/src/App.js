import logo from './logo.svg';
import './App.css';

function App() {
    const name = '리액트';
    const style = {
        backgroundColor: 'black', // background-color => backgroundColor
        color: 'aqua',
        fontSize: '48px', // font-size => fontSize
        fontWeight: 'bold', // font-weight => fontWeight
        padding: 16 // 단위 생략시 px로 적용
    }
    return (
        <div style={style}>{name}</div>
    )
}

export default App;