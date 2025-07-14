package forms4s.example.docs

import org.scalatest.funsuite.AnyFunSuite

import java.io.{File, PrintWriter}
import scala.io.Source
import scala.util.Using

class JsonDocGenTest extends AnyFunSuite {

  private val outputPath = "forms4s-examples/src/test/resources/docs/form-state-examples.md"

  test("JsonDocGen generates correct documentation and matches snapshot") {
    // Generate new content
    val newContent = JsonDocGen.generateDocumentation()

    val file            = new File(outputPath)
    val existingContent =
      if (file.exists()) Using(Source.fromFile(file))(_.mkString).get
      else ""

    file.getParentFile.mkdirs()
    Using(new PrintWriter(file))(_.write(newContent)).get

    assert(existingContent == newContent, "Content has changed. Please review the changes and commit the updated file.")
  }
}
