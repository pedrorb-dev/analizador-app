# Guía de explicación del analizador (defensa oral)

Material de apoyo para explicar el proyecto en la evaluación (rúbrica:
"cualquiera del equipo explica el analizador"). Son puntos para responder en
voz alta, en lenguaje natural, partiendo del ejemplo `pruebas/valido.txt`.

## 1. ¿Qué hace el programa?

- Es un **analizador de un lenguaje de programación pequeño**. Al elegir un
  archivo `.txt` con un programa, la GUI genera cinco resultados:
  1. Lista de tokens (`.tok`): el análisis **léxico**.
  2. Tabla de símbolos (`.tab`): los nombres encontrados y sus referencias
     (REF), más la tabla semántica al final cuando el programa es correcto.
  3. Archivo de dependencias `.dep`.
  4. Árbol de análisis sintáctico (`.arb`).
  5. En pantalla: análisis **sintáctico** y **semántico** con sus errores.
- Esas son las tres fases clásicas de un compilador: léxica, sintáctica y
  semántica.

## 2. Fase léxica (Lexer.java)

- Recorre el texto **carácter a carácter** y agrupa los caracteres en
  **tokens** (palabras reservadas, identificadores, números, cadenas,
  operadores, paréntesis, punto y coma).
- Lleva cuenta de la **línea (`yyline`) y la columna (`ultimaColumna`)**
  donde empieza cada token, porque después todos los errores se reportan con
  "Renglón: X, Columna: Y".
- Reporta errores léxicos (caracteres no válidos, cadenas sin cerrar) y
  valida además punto y coma y paréntesis.
- Se escribió **a mano**; también existe `Lexer.flex` (especificación JFlex)
  solo como referencia, no se usa.

## 3. Fase sintáctica (Parser.java + NodoArbol.java)

- Es un **parser descendente recursivo** escrito a mano.
- Aplica las **gramáticas del lenguaje**: `programa`, `declaraciones`,
  `instrucciones`, `asignación`, `si`, `while`, `expresión`, etc.
- Con cada regla construye un **árbol de análisis sintáctico (AST)** de nodos
  (`NodoArbol`): la raíz es `PROGRAMA`, y de ahí cuelgan la sección de
  variables, la sección de código y el fin.
- Cada nodo guarda la línea y la columna del token que lo originó.
- Si el programa no cumple la gramática, lanza un error sintáctico con su
  renglón y columna, y el árbol no se analiza semánticamente (el semántico
  solo corre si el sintáctico terminó bien).

## 4. Fase semántica (AnalizadorSemantico.java)

Es el núcleo de la práctica y hace el trabajo en **una sola pasada** sobre el
árbol. Tiene tres estructuras:

### 4.1 Tabla de símbolos
- Mapa `variable -> tipo` (`entero/cadena/booleano`).
- Se **alimenta** cuando se visita la sección de declaraciones (`varent`,
  `varcad`, `varbool`).
- Se **consulta** cada vez que se usa una variable (asignación, condición,
  lectura o impresión): si no está declarada, error.
- Detecta **doble declaración**.

### 4.2 Pila de ámbitos (alcance)
- Al entrar al cuerpo de un `si` o un `while` se abre un **ámbito** nuevo
  (`entrarAmbito()`); al terminar se cierra (`salirAmbito()`).
- La búsqueda de una variable va del ámbito **más interno hacia el externo**;
  así se respeta el alcance (scope) de las variables.

### 4.3 Pila semántica (evaluación de expresiones)
- Las expresiones se evalúan **de abajo hacia arriba** con una pila de
  valores (la pila semántica) y una pila de operadores.
- Los operandos se **apilan**; cuando un operador tiene prioridad suficiente,
  se desapilan dos operandos, se aplica y se vuelve a apilar el resultado
  (regla de prioridad `* /` sobre `+ -`).
- Si los dos operandos son **constantes**, la operación se resuelve en tiempo
  de análisis: **plegado de constantes** (p. ej. `printInt(4 + 6)` se muestra
  evaluado como `10`). Esto también detecta errores como divisiones entre
  cero.

### 4.4 Reglas de tipos que verifica
- Aritméticas `+ - * /`: solo enteros.
- Relacionales `==` y `!=`: operandos del mismo tipo.
- Relacionales `>` y `<`: solo enteros.
- `printInt(...)`: argumento entero. `printBool(...)`: booleano.
  `ponerConsola(...)`: cualquier tipo.
- `leerent`/`leercad`/`leerbol`: la variable debe ser entero/cadena/booleano.
- Asignación: el tipo de la expresión debe coincidir con el de la variable.
- Las condiciones de `si`/`while` producen un valor booleano.

### 4.5 Errores
- Se juntan **todos** y se muestran al final (no se detiene en el primero),
  cada uno con "Renglón: X, Columna: Y, Error semántico: descripción".

## 5. Un ejemplo paso a paso (`pruebas/valido.txt`)

- Léxico: de cada línea salen tokens con su renglón y columna.
- Sintáctico: se arma el AST PROGRAMA con sus secciones.
- Semántico (una pasada):
  1. Declaraciones: `a`, `b` -> entero; `mensaje` -> cadena; `bandera` ->
     booleano (se alimenta la tabla de símbolos).
  2. `a = 10;` -> tipo entero correcto, y como `10` es constante, queda
     anotada la evaluación `a = 10 => 10 (entero)`.
  3. `b = a * 2 + 3;` -> se aplica la prioridad: `a*2` primero; tipos válidos.
  4. `printInt(4 + 6);` -> evaluado por plegado: `10`.
  5. `si (9 > 4)` -> condición constante evaluada a `true` (booleano).
  6. El cuerpo del `si` y del `while` abren un ámbito nuevo y lo cierran.

## 6. Preguntas rápidas para la defensa

- **¿Por qué el análisis semántico es correcto?** Porque verifica en una sola
  pasada que todo lo que se usa exista y sea del tipo correcto, usando tabla
  de símbolos, ámbito y pila semántica.
- **¿Qué pasa si hay un error léxico/sintáctico?** No se llega al semántico;
  se reportan los errores con su renglón y columna.
- **¿Cómo respeta el alcance?** Con la pila de ámbitos: dentro de `si`/`while`
  se busca primero el ámbito interno y luego el externo.
- **¿Qué es el plegado de constantes?** Evaluar en tiempo de análisis las
  expresiones cuyos operandos son literales, mostrando su resultado.
- **¿Una sola pasada?** Sí: la declaración ocurre antes que el uso en el
  árbol, por lo que al recorrer una sola vez la sección de variables y luego
  la de código ya hay suficiente contexto para todo.