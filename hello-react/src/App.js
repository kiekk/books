import logo from './logo.svg';
import './App.css';

function App() {
    const name = '리액트'
    return (
        <>
            {name === '리액트' ? (
                <h1>Hello {name}!</h1>
            ) : (
                <h1>Is Not React!</h1>
            )}
            <h2>React Test</h2>
        </>
    );
}

export default App;