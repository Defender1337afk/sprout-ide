package com.sprout.ide.lang

enum class Syntax { CLike, Python, Markup, Css, Json, Markdown, Shell, Sql, Yaml, Lua, Ruby, Plain }

class Language(
    val id: String,
    val name: String,
    val badge: String,
    val hue: Double,
    val extensions: List<String>,
    val syntax: Syntax,
    val keywords: Set<String> = emptySet(),
    val types: Set<String> = emptySet(),
    val constants: Set<String> = setOf("true", "false", "null"),
    val lineComment: String? = "//",
    val blockComment: Pair<String, String>? = "/*" to "*/",
    val stringDelims: String = "\"'",
    val tripleQuotes: Boolean = false,
    val template: (String) -> String = { "" },
    val runnable: Boolean = false,
) {
    val defaultExt get() = extensions.first()
}

private fun words(s: String) = s.trim().split(Regex("\\s+")).toSet()

object Languages {
    private val cTypes = words("int long short char float double void bool unsigned signed size_t auto const static struct enum union typedef")

    val Kotlin = Language(
        "kotlin", "Kotlin", "KT", 280.0, listOf("kt", "kts"), Syntax.CLike,
        keywords = words("""package import class interface object fun val var if else when for while do return break continue
            |try catch finally throw in is as by get set init constructor companion data sealed enum open abstract override
            |private public protected internal inline suspend operator infix lateinit const typealias this super out reified vararg
            |annotation inner tailrec expect actual value""".trimMargin()),
        types = words("Int Long Short Byte Float Double Boolean Char String Unit Any Nothing List Map Set Array MutableList MutableMap Pair"),
        stringDelims = "\"'", tripleQuotes = true,
        template = { n -> "fun main() {\n    println(\"Привет из $n!\")\n}\n" },
    )
    val Java = Language(
        "java", "Java", "JV", 30.0, listOf("java"), Syntax.CLike,
        keywords = words("""package import class interface enum extends implements new return if else for while do switch case default
            |break continue try catch finally throw throws public private protected static final abstract synchronized volatile
            |transient native this super instanceof var record sealed permits yield void""".trimMargin()),
        types = words("int long short byte float double boolean char String Object Integer Long Double Boolean List Map Set"),
        template = { n ->
            val cls = n.substringBeforeLast('.').replaceFirstChar { it.uppercase() }
            "public class $cls {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n    }\n}\n"
        },
    )
    val Python = Language(
        "python", "Python", "PY", 230.0, listOf("py", "pyw"), Syntax.Python,
        keywords = words("""def class return if elif else for while break continue pass import from as with try except finally raise
            |lambda yield global nonlocal assert del in is not and or async await match case""".trimMargin()),
        types = words("int float str bool list dict set tuple bytes object type print len range self cls"),
        constants = words("True False None"),
        lineComment = "#", blockComment = null, tripleQuotes = true,
        template = { _ -> "def main():\n    print(\"Hello, World!\")\n\n\nif __name__ == \"__main__\":\n    main()\n" },
    )
    val JavaScript = Language(
        "javascript", "JavaScript", "JS", 95.0, listOf("js", "mjs", "cjs", "jsx"), Syntax.CLike,
        keywords = words("""var let const function return if else for while do switch case default break continue new delete typeof
            |instanceof in of class extends super this import export from as async await yield try catch finally throw""".trimMargin()),
        types = words("Array Object String Number Boolean Promise Map Set JSON Math console document window"),
        constants = words("true false null undefined NaN Infinity"),
        stringDelims = "\"'`",
        template = { _ -> "function greet(name) {\n  return `Привет, \${name}!`;\n}\n\nconsole.log(greet(\"мир\"));\n" },
        runnable = true,
    )
    val TypeScript = Language(
        "typescript", "TypeScript", "TS", 250.0, listOf("ts", "tsx"), Syntax.CLike,
        keywords = JavaScript.keywords + words("interface type enum implements private public protected readonly declare namespace abstract keyof is"),
        types = words("string number boolean any unknown never void object Array Promise Record Partial"),
        constants = JavaScript.constants, stringDelims = "\"'`",
        template = { _ -> "function greet(name: string): string {\n  return `Hello, \${name}!`;\n}\n\nconsole.log(greet(\"world\"));\n" },
    )
    val Html = Language(
        "html", "HTML", "<>", 20.0, listOf("html", "htm"), Syntax.Markup, lineComment = null, blockComment = "<!--" to "-->",
        template = { _ ->
            """<!DOCTYPE html>
            |<html lang="ru">
            |<head>
            |  <meta charset="UTF-8">
            |  <meta name="viewport" content="width=device-width, initial-scale=1.0">
            |  <title>Sprout</title>
            |  <link rel="stylesheet" href="style.css">
            |</head>
            |<body>
            |  <h1>Привет, мир!</h1>
            |  <script src="script.js"></script>
            |</body>
            |</html>
            |""".trimMargin()
        },
        runnable = true,
    )
    val Css = Language(
        "css", "CSS", "#", 200.0, listOf("css", "scss", "less"), Syntax.Css, lineComment = null,
        template = { _ -> "body {\n  font-family: system-ui, sans-serif;\n  margin: 0;\n  padding: 24px;\n  background: #f4fbf3;\n}\n\nh1 {\n  color: #1f6e36;\n}\n" },
    )
    val Json = Language("json", "JSON", "{}", 50.0, listOf("json"), Syntax.Json, lineComment = null, blockComment = null, stringDelims = "\"",
        template = { _ -> "{\n  \"name\": \"sprout\",\n  \"version\": \"1.0.0\"\n}\n" })
    val Xml = Language("xml", "XML", "</>", 170.0, listOf("xml", "svg", "plist"), Syntax.Markup, lineComment = null, blockComment = "<!--" to "-->",
        template = { _ -> "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<root>\n</root>\n" }, runnable = false)
    val Markdown = Language("markdown", "Markdown", "MD", 320.0, listOf("md", "markdown"), Syntax.Markdown, lineComment = null, blockComment = null,
        template = { n -> "# ${n.substringBeforeLast('.')}\n\nОписание проекта.\n\n## Запуск\n\n```bash\n./run.sh\n```\n" }, runnable = true)
    val C = Language("c", "C", "C", 215.0, listOf("c", "h"), Syntax.CLike,
        keywords = words("if else for while do switch case default break continue return goto sizeof typedef struct enum union extern static const volatile register inline #include #define #ifdef #ifndef #endif #pragma"),
        types = cTypes, constants = words("NULL true false"),
        template = { _ -> "#include <stdio.h>\n\nint main(void) {\n    printf(\"Hello, World!\\n\");\n    return 0;\n}\n" })
    val Cpp = Language("cpp", "C++", "C++", 330.0, listOf("cpp", "cc", "cxx", "hpp", "hh"), Syntax.CLike,
        keywords = C.keywords + words("class namespace using template typename public private protected virtual override new delete this try catch throw operator friend constexpr nullptr noexcept"),
        types = cTypes + words("std string vector map set unique_ptr shared_ptr"), constants = words("nullptr true false NULL"),
        template = { _ -> "#include <iostream>\n\nint main() {\n    std::cout << \"Hello, World!\" << std::endl;\n    return 0;\n}\n" })
    val CSharp = Language("csharp", "C#", "C#", 275.0, listOf("cs"), Syntax.CLike,
        keywords = words("using namespace class struct interface enum public private protected internal static readonly const new return if else for foreach while do switch case break continue try catch finally throw var async await override virtual abstract sealed this base in out ref is as get set record"),
        types = words("int long float double bool char string object void decimal byte List Dictionary Task"),
        template = { _ -> "using System;\n\nclass Program\n{\n    static void Main()\n    {\n        Console.WriteLine(\"Hello, World!\");\n    }\n}\n" })
    val Go = Language("go", "Go", "GO", 195.0, listOf("go"), Syntax.CLike,
        keywords = words("package import func return if else for range switch case default break continue go defer select chan map struct interface type var const fallthrough goto"),
        types = words("int int8 int16 int32 int64 uint uint8 uint16 uint32 uint64 float32 float64 string bool byte rune error any"),
        constants = words("true false nil iota"), stringDelims = "\"'`",
        template = { _ -> "package main\n\nimport \"fmt\"\n\nfunc main() {\n\tfmt.Println(\"Hello, World!\")\n}\n" })
    val Rust = Language("rust", "Rust", "RS", 35.0, listOf("rs"), Syntax.CLike,
        keywords = words("fn let mut const static if else match for while loop break continue return struct enum impl trait pub use mod crate self super where as ref move async await dyn unsafe type"),
        types = words("i8 i16 i32 i64 i128 isize u8 u16 u32 u64 u128 usize f32 f64 bool char str String Vec Option Result Box Self"),
        constants = words("true false None Some Ok Err"), stringDelims = "\"",
        template = { _ -> "fn main() {\n    println!(\"Hello, World!\");\n}\n" })
    val Swift = Language("swift", "Swift", "SW", 15.0, listOf("swift"), Syntax.CLike,
        keywords = words("import func let var if else guard switch case default for in while repeat return break continue class struct enum protocol extension init deinit self super public private internal fileprivate static override throws try catch async await"),
        types = words("Int Double Float String Bool Character Array Dictionary Set Any Optional Void"), constants = words("true false nil"),
        template = { _ -> "print(\"Hello, World!\")\n" })
    val Dart = Language("dart", "Dart", "DT", 210.0, listOf("dart"), Syntax.CLike,
        keywords = words("import library part class extends implements with mixin abstract final const var void return if else for while do switch case default break continue new this super async await yield try catch finally throw late required static get set enum"),
        types = words("int double num String bool List Map Set dynamic Object Future Stream Widget"),
        template = { _ -> "void main() {\n  print('Hello, World!');\n}\n" })
    val Php = Language("php", "PHP", "PHP", 260.0, listOf("php"), Syntax.CLike,
        keywords = words("function return if else elseif for foreach while do switch case default break continue class new public private protected static echo print namespace use extends implements try catch throw as array"),
        constants = words("true false null TRUE FALSE NULL"), lineComment = "//",
        template = { _ -> "<?php\n\necho \"Hello, World!\\n\";\n" })
    val Ruby = Language("ruby", "Ruby", "RB", 5.0, listOf("rb"), Syntax.Ruby,
        keywords = words("def end class module if elsif else unless while until for in do return yield begin rescue ensure raise then case when require attr_accessor self puts"),
        constants = words("true false nil"), lineComment = "#", blockComment = null,
        template = { _ -> "def greet(name)\n  puts \"Hello, #{name}!\"\nend\n\ngreet(\"World\")\n" })
    val Lua = Language("lua", "Lua", "LUA", 240.0, listOf("lua"), Syntax.Lua,
        keywords = words("function local end if then elseif else for in do while repeat until return break and or not"),
        constants = words("true false nil"), lineComment = "--", blockComment = "--[[" to "]]",
        template = { _ -> "local function greet(name)\n  print(\"Hello, \" .. name .. \"!\")\nend\n\ngreet(\"World\")\n" })
    val Shell = Language("shell", "Shell", "SH", 120.0, listOf("sh", "bash", "zsh"), Syntax.Shell,
        keywords = words("if then else elif fi for while do done case esac in function return export local echo exit source"),
        constants = emptySet(), lineComment = "#", blockComment = null,
        template = { _ -> "#!/usr/bin/env bash\nset -euo pipefail\n\necho \"Hello, World!\"\n" })
    val Sql = Language("sql", "SQL", "SQL", 185.0, listOf("sql"), Syntax.Sql,
        keywords = words("select from where insert into values update set delete create table drop alter add primary key foreign references join left right inner outer on group by order having limit as and or not null distinct union index view"),
        types = words("int integer text varchar char real boolean date timestamp blob"),
        lineComment = "--",
        template = { _ -> "CREATE TABLE users (\n  id INTEGER PRIMARY KEY,\n  name TEXT NOT NULL\n);\n\nSELECT * FROM users;\n" })
    val Yaml = Language("yaml", "YAML", "YML", 0.0, listOf("yml", "yaml", "toml", "ini", "properties", "conf"), Syntax.Yaml,
        lineComment = "#", blockComment = null, template = { _ -> "name: sprout\nversion: 1.0.0\n" })
    val Plain = Language("text", "Текст", "TXT", 140.0, listOf("txt", "log", "csv"), Syntax.Plain, lineComment = null, blockComment = null)

    val all = listOf(
        Kotlin, Java, Python, JavaScript, TypeScript, Html, Css, Json, Xml, Markdown,
        C, Cpp, CSharp, Go, Rust, Swift, Dart, Php, Ruby, Lua, Shell, Sql, Yaml, Plain,
    )

    fun forFile(name: String): Language {
        val lower = name.lowercase()
        if (lower == "makefile" || lower == "dockerfile" || lower.startsWith(".") && '.' !in lower.drop(1)) return Shell
        val ext = lower.substringAfterLast('.', "")
        return all.firstOrNull { ext in it.extensions } ?: Plain
    }
}
