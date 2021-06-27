package moe.kouyou.momofastrun

import org.bukkit.Bukkit
import org.bukkit.command.*
import org.bukkit.plugin.java.JavaPlugin
import java.io.*
import javax.tools.*

class MomoFastrun : JavaPlugin() {
  companion object {
    lateinit var instance: MomoFastrun
    lateinit var classloader: ClassLoader
  }

  override fun onEnable() {
    instance = this
    classloader = classLoader
    bcl = BytecodeClassLoader(classloader)
    Bukkit.getPluginCommand("momofastrun").setExecutor(this)
    if (!dataFolder.exists()) {
      dataFolder.mkdirs()
    }
  }

  override fun onCommand(s: CommandSender?, c: Command?, l: String?, args: Array<out String>?): Boolean {
    if (!s!!.isOp) return true
    args!!
    if (args.size == 0) {
      s.sendMessage(arrayOf(
        "参数不足.",
        "/momofastrun <文件名> [方法名] [参数...]"
        ,"示例: /mfr Test.class main HelloWorld"
        ,"只能调用参数为String[]或无参的静态方法"))
      return true
    }
    val fileName: String = args[0]
    val method: String = if(args.size == 1) "main" else args[1]
    val runArg: Array<String> = if(args.size <= 2) emptyArray() else args.takeLast(args.size - 2).toTypedArray()
    run(load(read(find(fileName))), method, runArg)
    return true
  }

  fun find(name: String): File {
    if (!dataFolder.exists()) {
      dataFolder.mkdirs()
      throw RuntimeException()
    }
    val f = File(dataFolder, name)
    if (!f.exists()) {
      f.createNewFile()
      throw RuntimeException()
    }
    return f
  }

  fun read(f: File): ByteArray {
    val fis = f.inputStream()
    val result = fis.readBytes()
    fis.close()
    return result
  }

  lateinit var bcl: BytecodeClassLoader
  fun load(code: ByteArray): Class<*> = bcl.define(code)

  fun run(c: Class<*>, m: String, args: Array<String>) {
    val ms = c.declaredMethods.filter { it.name == m }.filter {
      if(it.parameterCount == 0) true
      else it.parameterCount == 1 &&  it.parameterTypes[0] == Array<String>::class.java
    }
    val met = ms[0]
    met.isAccessible = true
    if(met.parameterCount == 0) met.invoke(null)
    else met.invoke(null, args)
  }

}

class BytecodeClassLoader(p: ClassLoader) : ClassLoader(p) {
  fun define(code: ByteArray): Class<*> = super.defineClass(code, 0, code.size)
}
