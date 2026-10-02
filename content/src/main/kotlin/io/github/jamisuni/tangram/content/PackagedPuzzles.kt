package io.github.jamisuni.tangram.content

/** Reads the puzzle files that the `packagePuzzles` build task put on the classpath (G-08, DA-10). */
internal object PackagedPuzzles {

    private const val DIR = "tangrams/"

    fun files(loader: ClassLoader = PackagedPuzzles::class.java.classLoader!!): List<PuzzleFile> {
        val index = checkNotNull(loader.getResourceAsStream("${DIR}index.txt")) {
            "tangrams/index.txt is missing from the classpath: check the content build (packagePuzzles, processResources)"
        }.use { it.readBytes().toString(Charsets.UTF_8) }
        return index.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { name ->
            val stream = checkNotNull(loader.getResourceAsStream(DIR + name)) {
                "tangrams/index.txt lists $name but the resource is missing: check the content build (packagePuzzles, processResources)"
            }
            PuzzleFile(name, stream.use { it.readBytes().toString(Charsets.UTF_8) })
        }
    }
}
