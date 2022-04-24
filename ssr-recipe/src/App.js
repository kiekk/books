import Menu from "./components/Menu";
import {BrowserRouter, Route, Routes} from "react-router-dom";
import RedPage from "./pages/RedPage";
import BluePage from "./pages/BluePage";

function App() {
  return (
    <BrowserRouter>
      <Menu/>
      <hr/>
      <Routes>
        <Route path="/red" element={<RedPage/>}/>
        <Route path="/blue" element={<BluePage/>}/>
      </Routes>
    </BrowserRouter>
  )
}

export default App;
