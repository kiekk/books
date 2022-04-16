import NewsList from "./components/NewsList";
import Categories from "./components/Categories";
import {BrowserRouter} from "react-router-dom";
import {useCallback, useState} from "react";

const App = () => {
  const [category, setCategory] = useState('all');
  const onSelect = useCallback(category => setCategory(category), []);

  return (
    <BrowserRouter>
      <Categories category={category} onSelect={onSelect}/>
      <NewsList category={category}/>
    </BrowserRouter>
  )
}

export default App;
