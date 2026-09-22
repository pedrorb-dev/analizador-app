# Analizador

Analizador léxico, sintáctico y semántico para un lenguaje de programación educativo e imperativo, implementado en Java. El programa ofrece una interfaz gráfica (Swing) que recibe un archivo fuente, lo descompone en tokens, valida su gramática y tipos, y genera archivos de salida con los resultados del análisis.

## Características

- **Análisis léxico**: convierte el código fuente en una secuencia de tokens (palabras reservadas, identificadores, números, cadenas, operadores y símbolos).
- **Análisis sintáctico**: valida la estructura del programa según la gramática definida mediante un parser descendente recursivo y construye un árbol de análisis.
- **Análisis semántico**: comprueba la corrección de tipos y reglas que no se pueden verificar durante el análisis sintáctico (variables declaradas y usadas correctamente, tipos de expresiones, condiciones booleanas).
- **Detección de errores**:
  - Léxicos: cadenas sin cerrar, número seguido de letras, identificadores con caracteres no ASCII o que empiezan con guion bajo, símbolos no válidos.
  - Estructurales: punto y coma (`;`) faltante entre líneas, paréntesis desbalanceados, comillas sin cerrar.
  - Sintácticos: instrucciones o expresiones mal formadas.
  - Semánticos: variable no declarada o declarada dos veces, tipos incompatibles, operandos no enteros en expresiones aritméticas, condiciones no booleanas.
- **Tabla de símbolos**: asigna referencias (REF) únicas a tokens relevantes (reservadas `100+`, identificadores `200+`, números `300+`, cadenas `400+`, errores `1900+`).
- **Comentarios**: soporta `//` (una línea) y `/* ... */` (multilínea).
- **Entrada y salida por archivos**: genera un `.tok`, `.tab`, `.dep` y `.arb` junto al archivo fuente analizado.

## Lenguaje soportado

El lenguaje analizado tiene la siguiente estructura:

- Todo programa inicia con `p#`.
- Sección `variables`: declaraciones con `varent`, `varcad` y `varbool`.
- Sección `codigo`: instrucciones del programa.
- Todo programa termina con `fin`.

### Palabras reservadas

| Palabra | Uso |
| --- | --- |
| `p#` | Inicio de programa |
| `variables` | Inicia la sección de declaraciones |
| `varent`, `varcad`, `varbool` | Declaración de variables (entero, cadena, booleano) |
| `codigo` | Inicia la sección de instrucciones |
| `ponerConsola` | Muestra un valor en consola (cualquier tipo) |
| `printInt` | Muestra un valor, que debe ser de tipo entero |
| `printBool` | Muestra un valor, que debe ser de tipo booleano |
| `leerent`, `leercad`, `leerbol` | Lectura de un valor (entero, cadena, booleano) |
| `si`, `entonces`, `fin` | Estructura condicional con condición relacional |
| `while`, `fin` | Bucle mientras se cumpla la condición relacional |
| `fin` | Termina el programa o un bloque condicional/bucle |

### Ejemplo de programa

```
p#
variables
varent a;
varcad mensaje;
codigo
a = 5 + 3 * 2;
mensaje = "hola";
printInt(a);
si (a > 10) entonces
    ponerConsola(mensaje);
fin
while (a > 0)
    a = a - 1;
fin
fin
```

### Tipos y reglas semánticas

- Tipos: `varent` → entero (`INT`), `varcad` → cadena (`CADENA`) y `varbool` → booleano (`BOOL`).
- Toda variable usada en el cuerpo debe estar declarada, y ninguna puede declararse dos veces.
- En una asignación, el tipo de la variable y el de la expresión deben coincidir.
- `+`, `-`, `*` y `/`: ambos operandos deben ser `INT` y el resultado es `INT`.
- `==` y `!=`: ambos operandos deben tener el mismo tipo; el resultado es `BOOL`.
- `>` y `<`: ambos operandos deben ser `INT`; el resultado es `BOOL`.
- `printInt` exige una expresión `INT`; `printBool` exige una expresión `BOOL`; `ponerConsola` admite cualquier tipo.
- `leerent` exige una variable `INT`, `leercad` una `CADENA` y `leerbol` una `BOOL`.
- La condición de `si`/`while` es siempre una comparación relacional y, por tanto, de tipo `BOOL`.

### Operadores

- Aritméticos: `+`, `-`, `*`, `/`
- Relacionales: `==`, `!=`, `>`, `<`
- Asignación: `=`

### Símbolos

- Paréntesis: `(`, `)`
- Llaves: `{`, `}`
- Punto y coma: `;`

## Estructura del proyecto

| Archivo | Descripción |
| --- | --- |
| `src/FramePrincipal.java` | Interfaz gráfica y clase principal (`main`). Orquesta el análisis y genera los archivos de salida. |
| `src/Lexer.java` | Analizador léxico implementado a mano. Produce los tokens a partir del texto de entrada. |
| `src/Lexer.flex` | Especificación JFlex equivalente del analizador léxico (referencia para regenerar `Lexer`). |
| `src/Tokens.java` | Enum con los tipos de tokens del lenguaje. |
| `src/TokenData.java` | Contenedor de datos de un token (lexema, tipo, línea, referencia). |
| `src/Parser.java` | Analizador sintáctico descendente recursivo. Valida la estructura y construye el árbol. |
| `src/NodoArbol.java` | Representación de los nodos del árbol de análisis sintáctico (con línea de código). |
| `src/AnalizadorSemantico.java` | Analizador semántico. Recorre el árbol, construye la tabla de tipos y reporta errores. |
| `lib/` | Dependencias de referencia (JFlex y CUP). |
| `build.xml`, `nbproject/` | Proyecto de compilación con Ant/NetBeans. |
| `.idea/` | Configuración del proyecto para IntelliJ IDEA. |

## Requisitos

- JDK 22 o superior (el proyecto compila con `javac.source`/`javac.target` = 22).
- No requiere dependencias externas en tiempo de ejecución.

## Compilar y ejecutar

### Con NetBeans

Abrir el proyecto y ejecutar la clase principal `FramePrincipal` (o usar **Run Project**).

### Con Ant

```bash
ant clean
ant jar
java -jar dist/Analizador.jar
```

### Con línea de comandos

```bash
javac -encoding UTF-8 -d build/src  # compilar los archivos de src/
java -cp build FramePrincipal
```

## Uso

1. Ejecutar el programa y presionar **Analizar**.
2. Seleccionar el archivo fuente a analizar.
3. El resultado del análisis se muestra en pantalla y se generan los siguientes archivos en la misma carpeta del archivo fuente:

| Archivo | Contenido |
| --- | --- |
| `<nombre>.tok` | Lista de tokens encontrados (renglón, lexema, token, REF) y errores detectados. |
| `<nombre>.tab` | Tabla de símbolos (número, lexema, token, REF). |
| `<nombre>.dep` | Código fuente sin espacios ni saltos de línea. |
| `<nombre>.arb` | Árbol de análisis sintáctico. |

## Flujo del análisis

1. Se lee el archivo fuente seleccionado.
2. `Lexer.yylex()` recorre el texto y devuelve los tokens uno a uno.
3. Durante el recorrido se valida el uso de punto y coma, el balance de paréntesis y las cadenas abiertas.
4. Se genera la tabla de símbolos asignando REF únicos por lexema.
5. `Parser.programa()` valida la estructura sintáctica y construye el árbol.
6. `AnalizadorSemantico.analizar()` recorre el árbol y comprueba declaraciones, uso de variables y tipos; si detecta errores, los muestra todos juntos.
7. Los resultados se muestran en pantalla (análisis semántico y sintáctico) y se guardan en los archivos de salida.

## Notas técnicas

- El lexer (`.flex`) y el parser podrían generarse con las herramientas JFlex y JavaCUP (incluidas en `lib/`), pero la implementación actual está escrita a mano, lo que facilita la depuración y el control del proceso de análisis.
- No existen tests automatizados; la validación se realiza analizando archivos fuente de ejemplo.