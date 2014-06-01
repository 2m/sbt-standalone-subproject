package sbt

import sbt.compiler.Eval
import sbt.BuildPaths._
import sbt.inc.{Locate, FileValueCache}
import sbt.BuildStreams._
import scala.Some

object CustomLoader {

  // copied from sbt.Load.defaultLoad
  def defaultLoad(state: State, baseDirectory: File, log: Logger, isPlugin: Boolean = false, topLevelExtras: List[URI] = Nil): (() => Eval, sbt.BuildStructure) =
  {
    val globalBase = getGlobalBase(state)
    val base = baseDirectory.getCanonicalFile
    val definesClass = FileValueCache(Locate.definesClass _)
    val rawConfig = Load.defaultPreGlobal(state, base, definesClass.get, globalBase, log)
    val config = Load.defaultWithGlobal(state, base, rawConfig, globalBase, log)
    val result = apply(base, state, config)
    definesClass.clear()
    result
  }

  // copied from sbt.Load.apply
  def apply(rootBase: File, s: State, config: sbt.LoadBuildConfiguration): (() => Eval, sbt.BuildStructure) =
  {
    // load, which includes some resolution, but can't fill in project IDs yet, so follow with full resolution
    val loaded = Load.resolveProjects(load(rootBase, s, config))
    val projects = loaded.units
    lazy val rootEval = Load.lazyEval(loaded.units(loaded.root).unit)
    val settings = Load.finalTransforms(Load.buildConfigurations(loaded, Load.getRootProject(projects), config.injectSettings))
    val delegates = config.delegates(loaded)
    val data = Def.make(settings)(delegates, config.scopeLocal, Project.showLoadingKey( loaded ) )
    val index = Load.structureIndex(data, settings, loaded.extra(data), projects)
    val streams = mkStreams(projects, loaded.root, data)
    (rootEval, new sbt.BuildStructure(projects, loaded.root, settings, data, index, streams, delegates, config.scopeLocal))
  }

  // copied from sbt.Load.load
  def load(file: File, s: State, config: sbt.LoadBuildConfiguration): sbt.PartBuild =
    Load.load(file, builtinLoader(s, config.copy(pluginManagement = config.pluginManagement.shift, extraBuilds = Nil)), config.extraBuilds.toList )

  // copied from sbt.Load.builtinLoader but changed BuildLoader.components(full = ...) parameter
  def builtinLoader(s: State, config: sbt.LoadBuildConfiguration): BuildLoader =
  {
    val fail = (uri: URI) => sys.error("Invalid build URI (no handler available): " + uri)
    val resolver = (info: BuildLoader.ResolveInfo) => RetrieveUnit(info)
    val build = (info: BuildLoader.BuildInfo) => Some(() => Load.loadUnit(info.uri, info.base, info.state, info.config))
    val components = BuildLoader.components(resolver, build, full = componentLoader)
    BuildLoader(components, fail, s, config)
  }

  // copied from sbt.BuildLoader.componentLoader but added transformation on BuildUnit
  def componentLoader: BuildLoader.Loader = (info: BuildLoader.LoadInfo) => {
    import info.{components, config, staging, state, uri}
    val cs = info.components
    for {
      resolve <- cs.resolver(new BuildLoader.ResolveInfo(uri, staging, config, state))
      base = resolve()
      build <- cs.builder(new BuildLoader.BuildInfo(uri, base, config, state))
    } yield () => {
      val unit = addDependency(build())
      cs.transformer(new BuildLoader.TransformInfo(uri, base, unit, config, state))
    }
  }

  def addDependency(unit: BuildUnit) = {
    val newDefinitions = new LoadedDefinitions(
      unit.definitions.base,
      unit.definitions.target,
      unit.definitions.loader,
      unit.definitions.builds,
      unit.definitions.projects.map {
        case p: Project if p.id == "standalone-subproject" =>
          println("Adding project dependency.")
          p.dependsOn(ProjectRef(file("standalone-subproject-dependency").toURI, "standalone-subproject-dependency"))
            .settings(
              sbt.Keys.allDependencies := sbt.Keys.allDependencies.value.filterNot { moduleId =>
                moduleId.organization == "2m"
              }
            )
        case p: Project => p
      },
      unit.definitions.buildNames
    )

    new BuildUnit(
      unit.uri,
      unit.localBase,
      newDefinitions,
      unit.plugins
    )
  }
}
