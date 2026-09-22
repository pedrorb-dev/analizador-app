# AGENTS.md

Java (JDK 22) analizador léxico, sintáctico y semántico. Proyecto NetBeans/Ant, sin paquetes (todas las clases viven en el paquete por defecto bajo `src/`).

## Build y ejecución

- Compilación oficial: Ant (`build.xml` + `nbproject/`), vía NetBeans o `ant clean jar && java -jar dist/Analizador.jar`. No hay Maven/Gradle.
- Todos los archivos bajo `src/` sin `package` declarado. Compilar manualmente: `javac -encoding UTF-8 -d build src/*.java` (obligatorio `-encoding UTF-8`; las fuentes están en UTF-8 y se usan literales como `p#`).
- Clase principal (solo GUI Swing, sin args CLI): `FramePrincipal` (`src/FramePrincipal.java:561`).
- No existen tests ni suite de lint/typecheck.

## Arquitectura (no evidente por nombres)

- **Dos lexers coexisten**: `src/Lexer.java` (escrito a mano, ES el que se usa) y `src/Lexer.flex` (especificación JFlex como referencia). NO regenerar `Lexer.java` desde el `.flex`; sobrescribiría la implementación manual y rompería la compilación.
- El parser `src/Parser.java` es descendente recursivo escrito a mano. JavaCUP/jflex (`lib/`) NO están en el classpath de compilación (`javac.classpath=` vacío) y no se usan en tiempo de ejecución.
- Flujo: `FramePrincipal.analizarArchivo` → `Lexer.yylex()` (tokens) validando en el camino punto y coma, paréntesis y cadenas → `Parser.programa()` construye el árbol con `NodoArbol` → `AnalizadorSemantico.analizar()` recorre el árbol y reporta errores semánticos (todos juntos). El análisis semántico solo se ejecuta si el sintáctico terminó bien.
- `AnalizadorSemantico.java`: pase en **una sola pasada**. Usa **tabla de símbolos** (alimentada al declarar, consultada al usar), **pila de ámbitos** (SI/MIENTRAS abren `entrarAmbito()/salirAmbito()`) y **pila semántica** (evaluación de expresiones con plegado de constantes mediante pila de operadores). Expone `obtenerTablaSimbolos()` y `obtenerEvaluaciones()`; `tipoEnEspanol()` traduce tipos para la UI.
- Columna: `Lexer` lleva `inicioLinea`/`ultimaColumna`; `TokenData.columna` y `NodoArbol.columna` propagan la posición; todos los errores (léxicos, sintácticos y semánticos) reportan "Renglón: X, Columna: Y".
- Salidas `.tok`, `.tab`, `.dep` y `.arb` se escriben en la carpeta del **archivo fuente analizado** (elegido con JFileChooser), no en el repo. El `.tab` incluye un anexo con la tabla de símbolos semántica cuando el análisis es correcto.

## Convenciones / gotchas

- Labels, mensajes, nombres de archivo de salida y errores están en español. Mantener ese idioma en mensajes de error y UI.
- El enum `Tokens` define el vocabulario; las palabras reservadas del lenguaje incluyen `p#`, `varent`, `varcad`, `varbool`, `ponerConsola`, `printInt`, `printBool`, `leerent`, `leercad`, `leerbol`, `si`, `entonces`, `while`, `fin`, `variables`, `codigo`.
- Comentarios: la regla general es no añadir comentarios salvo que se pidan, pero `AnalizadorSemantico.java` SÍ lleva Javadoc/comentarios en español de forma deliberada (lo exige la rúbrica de la práctica). Mantenerlos al tocar ese archivo.
- `.github/modernize/` contiene hooks de automatización de OpenCode, no forma parte del build.
- `build/` y `out/` contienen clases compiladas asumiendo que son ignorables/regenerables.
- `pruebas/` reúne casos de prueba `.txt` (programas válidos y con errores semánticos) para probar desde la GUI.