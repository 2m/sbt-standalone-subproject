package sbt
package experimental
import sbt.Keys._

/** Extend sbt.Project with a .dependsOnLocal method (allows depending on a local version of a project
 *  with convenient fallback to a published version) */
object DependsOnLocal {
  private def propPrefix(build: URI): String =
    build.toASCIIString.replaceAll("[\\W]+", ".")
  // Note - We disable builds COMPLETELY by file.  i.e. NO HOOKS allowed
  // if one hook is disabled...
  private def disableProperty(p: ProjectReference): String = p match {
    case ProjectRef(build, project) => s"sbt.${propPrefix(build)}disabled"
    case RootProject(build) => s"sbt.${propPrefix(build)}disabled"
    case _ => sys.error(s"Unable to disable project connection: $p")
  }
  def isLocalDisabled(p: ProjectReference): Boolean = {
    val property = disableProperty(p)
    val result = Option(sys.props(property)).map(java.lang.Boolean.parseBoolean).getOrElse(false)
    System.err.println(s"Checking ${p}[${property}] for disabled: ${result}")
    result
  }
  def setLocalDisabled(p: ProjectReference, disabled: Boolean = true): Unit =
    sys.props(disableProperty(p)) = if(disabled) "true" else "false"

  def dependsOnLocal(p: Project, other: ProjectReference, fallback: ModuleID): Project = 
    if (!isLocalDisabled(other))
        p.dependsOn(other)
      else
        p.settings(libraryDependencies += fallback)


  implicit class DepLocalProject(val p: Project) extends AnyVal {
    def dependsOnLocal(other: ProjectReference, fallback: ModuleID): Project = {
      DependsOnLocal.dependsOnLocal(p, other, fallback)
    }
  }
}
