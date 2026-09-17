# AGENTS.md

Java (JDK 22) analizador léxico y sintáctico. Proyecto NetBeans/Ant, sin paquetes (todas las clases viven en el paquete por defecto bajo `src/`).

## Build y ejecución

- Compilación oficial: Ant (`build.xml` + `nbproject/`), vía NetBeans o `ant clean jar && java -jar dist/Analizador.jar`. No hay Maven/Gradle.
- Todos los archivos bajo `src/` sin `package` declarado. Compilar manualmente: `javac -encoding UTF-8 -d build src/*.java` (obligatorio `-encoding UTF-8`; las fuentes están en UTF-8 y se usan literales como `p#`).
- Clase principal (solo GUI Swing, sin args CLI): `FramePrincipal` (`src/FramePrincipal.java:464`).
- No existen tests ni suite de lint/typecheck.

## Arquitectura (no evidente por nombres)

- **Dos lexers coexisten**: `src/Lexer.java` (escrito a mano, ES el que se usa) y `src/Lexer.flex` (especificación JFlex como referencia). NO regenerar `Lexer.java` desde el `.flex`; sobrescribiría la implementación manual y rompería la compilación.
- El parser `src/Parser.java` es descendente recursivo escrito a mano. JavaCUP/jflex (`lib/`) NO están en el classpath de compilación (`javac.classpath=` vacío) y no se usan en tiempo de ejecución.
- Flujo: `FramePrincipal.analizarArchivo` → `Lexer.yylex()` (tokens) validando en el camino punto y coma, paréntesis y cadenas → `Parser.programa()` construye el árbol con `NodoArbol`.
- Salidas `.tok`, `.tab`, `.dep` y `.arb` se escriben en la carpeta del **archivo fuente analizado** (elegido con JFileChooser), no en el repo.

## Convenciones / gotchas

- Labels, mensajes, nombres de archivo de salida y errores están en español. Mantener ese idioma en mensajes de error y UI.
- El enum `Tokens` define el vocabulario; las palabras reservadas del lenguaje incluyen `p#`, `varent`, `varcad`, `varbool`, `ponerConsola`, `leerent`, `leercad`, `leerbol`, `si`, `entonces`, `fin`, `variables`, `codigo`.
- `.github/modernize/` contiene hooks de automatización de OpenCode, no forma parte del build.
- `build/` y `out/` contienen clases compiladas asumiendo que son ignorables/regenerables.