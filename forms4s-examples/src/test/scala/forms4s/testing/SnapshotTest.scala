package forms4s.testing

import org.scalatest.Assertions

import java.nio.file.{Files, Paths}

object SnapshotTest {

  private val testResourcesPath = {
    val targetDir = Paths
      .get(getClass.getResource("/").toURI)
      .getParent // forms4s-examples/.jvm/target/scala-x.x.x or forms4s-examples/target/scala-x.x.x
      .getParent // forms4s-examples/.jvm/target or forms4s-examples/target
      .getParent // forms4s-examples/.jvm or forms4s-examples

    // Handle cross-project layout (.jvm/.js subdirectories)
    val moduleDir =
      if (targetDir.getFileName.toString == ".jvm" || targetDir.getFileName.toString == ".js")
        targetDir.getParent
      else
        targetDir

    moduleDir.resolve("src/test/resources")
  }

  def testSnapshot(content: String, path: String): Unit = {
    val filePath    = testResourcesPath.resolve(path)
    val existingOpt = Option.when(Files.exists(filePath)) {
      Files.readString(testResourcesPath.resolve(path))
    }

    val isOk = existingOpt.contains(content)

    if (!isOk) {
      Files.createDirectories(filePath.getParent)
      Files.writeString(filePath, content)
      Assertions.fail(s"Snapshot $path was not matching. A new value has been written to $filePath.")
    }
  }
}
