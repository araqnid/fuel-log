package org.araqnid.fuellog.test

import com.google.common.io.MoreFiles
import com.google.common.io.RecursiveDeleteOption
import org.junit.rules.ExternalResource
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class NIOTemporaryFolder(private val parentFolder: Path = Paths.get(System.getProperty("java.io.tmpdir"))) : ExternalResource() {
    private lateinit var folder: Path

    val root
        get() = folder

    override fun before() {
        folder = createTemporaryFolderIn(parentFolder)
    }

    override fun after() {
        MoreFiles.deleteRecursively(folder, RecursiveDeleteOption.ALLOW_INSECURE)
    }

    /**
     * Returns a new fresh file with the given name under the temporary folder.
     */
    @Throws(IOException::class)
    fun newFile(fileName: String): Path {
        val file = root.resolve(fileName)
        Files.createFile(file)
        return file
    }

    /**
     * Returns a new fresh file with a random name under the temporary folder.
     */
    @Throws(IOException::class)
    fun newFile(): Path = Files.createTempFile(root, "junit", null)

    /**
     * Returns a new fresh folder with a random name under the temporary folder.
     */
    @Throws(IOException::class)
    fun newFolder(): Path = createTemporaryFolderIn(root)

    /**
     * Returns a new fresh folder with the given name(s) under the temporary
     * folder.
     */
    @Throws(IOException::class)
    fun newFolder(vararg folderNames: String): Path {
        var file = root
        for (i in folderNames.indices) {
            val folderName = folderNames[i]
            validateFolderName(folderName)
            file = file.resolve(folderName)
            if (!Files.exists(file)) {
                Files.createDirectory(file)
            }
        }
        return file
    }

    /**
     * Validates if multiple path components were used while creating a folder.
     *
     * @param folderName
     * Name of the folder being created
     */
    private fun validateFolderName(folderName: String) {
        require(Paths.get(folderName).parent == null) { "Folder name cannot consist of multiple path components separated by a file separator." }
    }

    private fun createTemporaryFolderIn(parentFolder: Path): Path = Files.createTempDirectory(parentFolder, "junit")
}
