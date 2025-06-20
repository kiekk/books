import {useParams} from "react-router-dom";
import Categories from "./Categories";
import NewsList from "./NewsList";

const NewsPage = () => {
  const { category } = useParams();

  let cat = category || 'all';

  return (
    <>
      <Categories />
      <NewsList category={cat} />
    </>
  )
}

export default NewsPage;