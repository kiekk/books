import './App.css';

function App() {
  const name = 'React';
  return (
    <div>
      {name === 'React' ? (
        <h1>Hello React!</h1>
      ) : (
        <h1>This is not React!</h1>
      )}
    </div>
  );
}

export default App;
