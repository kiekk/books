import java.io.File

// 객체 선언은 object 키워드를 사용하며, 싱글톤 객체가 된다.
// 객체 선언문이 있는 위치에서 생성자 호출 없이 객체가 생성되기 때문에 주 생성자, 부 생성자를 사용할 수 없다.
object CaseInsensitiveFileComparator : Comparator<File> {
    override fun compare(file1: File, file2: File): Int {
        return file1.path.compareTo(
            file2.path,
            ignoreCase = true
        )
    }
}

fun main() {
    println(
        CaseInsensitiveFileComparator.compare(
            File("/User"), File("/user")
        )
    )
    val files = listOf(File("/Z"), File("/a"))
    println(files.sortedWith(CaseInsensitiveFileComparator))
}
