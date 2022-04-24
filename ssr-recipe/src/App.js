import React from 'react'
import Menu from "./components/Menu";
import {Route} from "react-router-dom";
import RedPage from "./pages/RedPage";
import BluePage from "./pages/BluePage";
import UsersPage from "./pages/UsersPage";

const App = () => {
  return (
    <div>
      <Menu/>
      <hr/>
      <Route path="/red" component={RedPage}/>
      <Route path="/blue" component={BluePage}/>
      <Route path="/users" component={UsersPage}/>
    </div>
  )
}

export default App;
