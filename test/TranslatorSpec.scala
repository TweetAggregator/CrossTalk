import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import controllers.Translator

@RunWith(classOf[JUnitRunner])
class TranslatorSpec extends Specification {
  
  "Translator" should {
    "translate" in new WithApplication {
      
      val from = "deu"
      val dest = "fra"
      val dest2 = "eng"
      val keyword = "Bier"

      val (translations, synonyms) = Translator(from, List(dest, dest2), keyword)()

      translations(1) should contain ("beer")
      translations(1) should contain ("ale")
      synonyms should contain ("Gerstensaft")
    }
  }
}
