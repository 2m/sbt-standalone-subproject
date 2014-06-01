import org.scalatest.WordSpec

class VersionSpec extends WordSpec {
  "version" should {
    "be printed out" in {
      println(s"\nCurrent version is: ${Version.version}\n")
    }
  }
}
