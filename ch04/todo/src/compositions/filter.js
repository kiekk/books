import { inject } from 'vue'

export const useFilter = () => {
  const today = inject('today')
  // 최근 날짜로 정렬
  const fnSort = (a, b) => {
    const a_date = Date.parse(a.date)
    const b_date = Date.parse(b.date)
    if (a_date > b_date) return 1
    else if (a_date < b_date) return 0
    else return a.id - b.id
  }

  // 오늘 해야 할 일들 중 완료하지 않은 작업
  const getPendingTodos = (todos) => {
    return todos.value
      .filter((todo) => todo.date < today && !todo.completed)
      .slice()
      .sort(fnSort)
  }

  // 오늘 해야 할 일들
  const getActiveTodayTodos = (todos) => {
    return todos.value
      .filter((todo) => todo.date == today && !todo.completed)
      .slice()
      .sort(fnSort)
  }

  // 오늘 해야 할 일들 중 완료한 작업
  const getCompletedTodayTodos = (todos) => {
    return todos.value
      .filter((todo) => todo.date == today && todo.completed)
      .slice()
      .sort(fnSort)
  }

  // 오늘 총 작업
  const getAllTodayTodos = (todos) => {
    return getActiveTodayTodos(todos)
      .concat(getCompletedTodayTodos(todos))
      .slice()
      .sort(fnSort)
  }

  // 총 작업업
 const getAllTodos = (todos) => {
    return todos.value.slice().sort(fnSort)
  }

  return {
    getPendingTodos,
    getActiveTodayTodos,
    getCompletedTodayTodos,
    getAllTodayTodos,
    getAllTodos,
  }
}
