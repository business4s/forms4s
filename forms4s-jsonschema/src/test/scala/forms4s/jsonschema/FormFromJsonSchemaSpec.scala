package forms4s.jsonschema

import forms4s.FormElement
import forms4s.FormElement.Text.Format
import org.scalatest.freespec.AnyFreeSpec
import sttp.tapir.Schema as TSchema
import sttp.tapir.Schema.annotations.validate
import sttp.tapir.Validator.Pattern
import sttp.tapir.docs.apispec.schema.TapirSchemaToJsonSchema
import sttp.tapir.generic.Configuration

import java.time.*
import java.util.UUID

class FormFromJsonSchemaSpec extends AnyFreeSpec {

  "convert derived schemas from Scala types" - {

    "simple product → Text, Number, Checkbox (all required)" in {
      case class Simple(a: String, b: Int, c: Boolean) derives TSchema
      val form = getForm[Simple]()

      val expected = FormElement.Group(
        simpleCore("Simple"),
        List(
          FormElement.Text(simpleCore("a", "A"), Format.Raw),
          FormElement.Number(simpleCore("b", "B"), isInteger = true),
          FormElement.Checkbox(simpleCore("c", "C")),
        ),
      )

      assert(form == expected)
    }

    "nested case class → Subform with Text inside" in {
      case class Address(street: String) derives TSchema
      case class User(address: Address) derives TSchema
      val form = getForm[User]()

      val expected = FormElement.Group(
        simpleCore("User"),
        List(
          FormElement.Group(
            simpleCore("address", "Address"),
            List(
              FormElement.Text(simpleCore("street", "Street"), Format.Raw),
            ),
          ),
        ),
      )

      assert(form == expected)
    }

    "integer" in {
      val form     = getForm[Int]()
      val expected = FormElement.Number(simpleCore("unknown", "Unknown"), isInteger = true)
      assert(form == expected)
    }
    "double" in {
      val form     = getForm[Int]()
      val expected = FormElement.Number(simpleCore("unknown", "Unknown"), isInteger = false)
      assert(form == expected)
    }

    "enum → Select with options" in {
      enum Color {
        case Red, Green, Blue
      }
      given TSchema[Color] = TSchema.derivedEnumeration.defaultStringBased
      val form             = getForm[Color]()

      val expected = FormElement.Select(
        simpleCore("_$Color"),
        options = List("Blue", "Green", "Red"),
      )

      assert(form == expected)
    }

    "optional object field" in {
      enum A {
        case A1, A2
      }
      given TSchema[A] = TSchema.derivedEnumeration.defaultStringBased
      case class Interim(a: Option[A]) derives TSchema
      case class Foo(x: Interim) derives TSchema
      val form1        = getForm[Foo](nullableOptions = true)
      // this doesnt work yet, its here to document the problem
      assert(form1 == null)
    }

    "list → Multivalue with Text inside" in {
      val form = getForm[List[String]]()

      val expected = FormElement.Multivalue(
        simpleCore("unknown", "Unknown"),
        item = FormElement.Text(simpleCore("unknown", "Unknown"), Format.Raw),
      )

      assert(form == expected)
    }

    "time" - {

      // TODO doesnt work due to missign support in tapir
      "OffsetTime" in {
        val form     = getForm[OffsetTime]()
        val expected = FormElement.Text(simpleCore("unknown", "Unknown"), Format.Time)
        assert(form == expected)
      }

      // TODO doesnt work due to missign support in tapir
      "LocalTime" in {
        val form     = getForm[LocalTime]()
        val expected = FormElement.Text(simpleCore("unknown", "Unknown"), Format.Time)
        assert(form == expected)
      }

      "LocalDate" in {
        val form     = getForm[LocalDate]()
        val expected = FormElement.Text(simpleCore("unknown", "Unknown"), Format.Date)
        assert(form == expected)
      }

      "Instant" in {
        val form     = getForm[Instant]()
        val expected = FormElement.Text(simpleCore("unknown", "Unknown"), Format.DateTime)
        assert(form == expected)
      }

      // TODO doesnt work due to missign support in tapir
      "LocalDateTime" in {
        val form     = getForm[LocalDateTime]()
        val expected = FormElement.Text(simpleCore("unknown", "Unknown"), Format.DateTime)
        assert(form == expected)
      }

      "OffsetDateTime" in {
        val form     = getForm[OffsetDateTime]()
        val expected = FormElement.Text(simpleCore("unknown", "Unknown"), Format.DateTime)
        assert(form == expected)
      }

      "ZonedDateTime" in {
        val form     = getForm[ZonedDateTime]()
        val expected = FormElement.Text(simpleCore("unknown", "Unknown"), Format.DateTime)
        assert(form == expected)
      }
    }

    "uuid" - {
      "through tapir" in {
        val form     = getForm[UUID]().asInstanceOf[FormElement.Text]

        assert(form.format == Format.Custom("uuid"))
        assert(form.core.validators.forall(_.validate(UUID.randomUUID().toString).isEmpty))
        assert(form.core.validators.exists(_.validate("ss").isDefined))
      }
    }
    // TODO doesnt work in tapir
    "java.time.Duration" - {
      "through tapir" in {
        val form     = getForm[java.time.Duration]()
        val expected = FormElement.Text(simpleCore("unknown", "Unknown"), Format.Custom("duration"))
        assert(form == expected)
      }
    }
    // TODO doesnt work in tapir
    "scala.concurrent.duration.Duration" - {
      "through tapir" in {
        val form     = getForm[scala.concurrent.duration.Duration]()
        val expected = FormElement.Text(simpleCore("unknown", "Unknown"), Format.Custom("duration"))
        assert(form == expected)
      }
    }

    "regex validator" in {
      case class WithFromat(@validate(Pattern("[a|b]")) a: String) derives TSchema
      val form = getForm[WithFromat]()

      val validators = form.asInstanceOf[FormElement.Group].elements.head.asInstanceOf[FormElement.Text].core.validators
      assert(validators.size == 1)
      assert(validators.head.validate("a") == None)
      assert(validators.head.validate("b") == None)
      assert(validators.head.validate("c") == Some("Value does not match format [a|b]"))
    }

    "sealed trait" - {
      "without discriminator" in {
        sealed trait Foo derives TSchema
        case class A(a: Int) extends Foo
        case class B(b: Int) extends Foo

        val form     = getForm[Foo]()
        val expected = FormElement.Alternative(
          simpleCore("Foo", "Foo"),
          List(
            FormElement.Group(simpleCore("A", "A"), List(FormElement.Number(simpleCore("a", "A"), true))),
            FormElement.Group(simpleCore("B", "B"), List(FormElement.Number(simpleCore("b", "B"), true))),
          ),
          None,
        )
        assert(form == expected)
      }
      "with discriminator" in {
        given Configuration = Configuration.default.withDiscriminator("tpe")
        sealed trait Foo derives TSchema
        case class A(a: Int) extends Foo
        case class B(b: Int) extends Foo

        val form     = getForm[Foo]()
        val expected = FormElement.Alternative(
          simpleCore("Foo", "Foo"),
          List(
            FormElement.Group(simpleCore("A", "A"), List(FormElement.Number(simpleCore("a", "A"), true))),
            FormElement.Group(simpleCore("B", "B"), List(FormElement.Number(simpleCore("b", "B"), true))),
          ),
          Some("tpe"),
        )
        assert(form == expected)
      }
    }

  }

  def getForm[T](nullableOptions: Boolean = false)(implicit tschema: TSchema[T]): FormElement = {
    val aschema = TapirSchemaToJsonSchema(tschema, markOptionsAsNullable = nullableOptions)
    FormFromJsonSchema.convert(aschema)
  }

  def simpleCore(name: String, label: String = null): FormElement.Core[Any] =
    FormElement.Core(name, Option(label).getOrElse(name), None, Seq())
}
