import NewsList from "./components/NewsList";
import Categories from "./components/Categories";
import {BrowserRouter} from "react-router-dom";

const App = () => {
  return (
    <BrowserRouter>
      <Categories />
      <NewsList />
    </BrowserRouter>
  )
}

export default App;
