package moe.kouyou.momofastrun

import org.bukkit.Bukkit
import org.bukkit.command.*
import org.bukkit.plugin.java.JavaPlugin
import java.io.*
import java.lang.invoke.*
import java.net.URLClassLoader
import java.nio.file.*
import javax.tools.*

class MomoFastrun : JavaPlugin() {
  lateinit var inst: MomoFastrun
  lateinit var ucl: URLClassLoader

  override fun onEnable() {
    inst = this
    reloadAll()
    initWatchService()
    Bukkit.getPluginCommand("momofastrun").setExecutor(this)
  }

  fun initWatchService() {
    val service = FileSystems.getDefault().newWatchService()
    Paths.get(dataFolder.toURI()).register(service, arrayOf(StandardWatchEventKinds.ENTRY_CREATE,
      StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY));
    Thread {
      while(inst.isEnabled) {
        val key = service.take()
        key.pollEvents()
        reloadAll()
        key.reset()
      }
    }.start()
    println("成功挂载了自动重载")
  }

  fun reloadAll() {
    if (!dataFolder.exists()) {
      dataFolder.mkdirs()
      return
    }
    methodCache.clear()
    ucl = URLClassLoader(arrayOf(dataFolder.toURI().toURL()), classLoader)
  }

  val lookup: MethodHandles.Lookup = MethodHandles.publicLookup()
  val methodCache: MutableMap<String, MethodHandle> = hashMapOf()
  fun findMethod(c: Class<*>, name: String): MethodHandle {
    return methodCache.getOrPut("${c.canonicalName}::$name") {
      val m = c.getDeclaredMethod(name, Array<String>::class.java)
      m.isAccessible = true
      return lookup.unreflect(m)
    }
  }

  override fun onCommand(s: CommandSender?, c: Command?, l: String?, args: Array<out String>?): Boolean {
    if (!s!!.isOp) return true
    args!!
    if (args.size == 0) {
      s.sendMessage(arrayOf("参数不足.", "/momofastrun <类名> [方法名] [参数...]",
        "示例: /mfr Test.class main HelloWorld", "只能调用参数为String[]的静态方法"))
      return true
    }
    val className: String = args[0]
    val method: String = if (args.size == 1) "main" else args[1]
    val runArg: Array<String> = if (args.size < 3) emptyArray() else args.takeLast(args.size - 2).toTypedArray()
    findMethod(ucl.loadClass(className), method).invoke(runArg)
    return true
  }

}
