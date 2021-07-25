import logo from './logo.svg';
import './App.css';

function App() {
    const name = '리액트'
    return (
        <>
            {name === '리액트' && <h1>Hello {name}!</h1>}
            {/* {name === '리액트' ? <h1>Hello {name}!</h1> : null } 과 동일하게 작동 */}
            <h2>React Test</h2>
        </>
    );
}

export default App;