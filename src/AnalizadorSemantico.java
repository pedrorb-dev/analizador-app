import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Análizador semántico del lenguaje.
 *
 * Recorre el árbol sintáctico (AST) en UNA SOLA PASADA y comprueba:
 *   - que toda variable usada esté declarada en el ámbito visible (alcance),
 *   - que los tipos de expresiones, asignaciones y condiciones sean válidos,
 *   - que las funciones (ponerConsola, printInt, printBool, leer*) reciban
 *     argumentos compatibles con su operación.
 *
 * Para ello trabaja con las dos estructuras clásicas del análisis semántico:
 *   - TABLA DE SÍMBOLOS: mapa (variable -> tipo) que se ALIMENTA al declarar
 *     cada variable y se CONSULTA cada vez que una variable se usa.
 *   - PILA SEMÁNTICA: pila de entradas (tipo + valor) con la que se evalúan
 *     las expresiones de abajo hacia arriba, aplicando PLEGADO DE CONSTANTES
 *     cuando ambos operandos son literales.
 *
 * El ÁMBITO se respeta con una pila de scopes: cada cuerpo de SI o MIENTRAS
 * abre un ámbito nuevo y las variables se buscan de adentro hacia afuera.
 *
 * Los errores se reportan todos juntos al final, indicando Renglón y Columna.
 */
public class AnalizadorSemantico {

    /** Los tres tipos de datos que maneja el lenguaje. */
    public enum Tipo {
        INT,
        CADENA,
        BOOL
    }

    /**
     * Entrada de la pila semántica.
     * Guarda el tipo calculado de la expresión y, si es constante,
     * su valor asociado (numérico, de cadena o booleano).
     */
    private static class EntradaSemantica {

        Tipo tipo;
        boolean constante;
        Integer valorInt;
        String valorCadena;
        Boolean valorBool;
    }

    /** Tabla de símbolos global: nombre de variable -> tipo. */
    private final Map<String, Tipo> tablaSimbolos = new HashMap<>();

    /**
     * Pila de ámbitos (scopes). La posición 0 es el ámbito global y la
     * última posición es el ámbito activo (el más interno).
     */
    private final List<Map<String, Tipo>> ambitos = new ArrayList<>();

    /** Pila semántica usada para evaluar las expresiones (bottom-up). */
    private final Deque<EntradaSemantica> pilaSemantica = new ArrayDeque<>();

    /** Errores semánticos detectados (con renglón y columna). */
    private final List<String> errores = new ArrayList<>();

    /** Evaluaciones resueltas por plegado de constantes, para mostrarlas. */
    private final List<String> evaluaciones = new ArrayList<>();

    // =====================================================================
    // Punto de entrada
    // =====================================================================

    /**
     * Analiza el programa completo en UNA SOLA PASADA sobre el árbol.
     *
     * Primero se recorre la SECCION VARIABLES (que alimenta la tabla de
     * símbolos) y después la SECCION CODIGO (que usa la tabla): así, en una
     * única pasada se declara y se verifica cada instrucción en orden.
     *
     * @param raiz raíz del árbol sintáctico (PROGRAMA)
     * @return lista de errores semánticos (vacía si el programa es válido)
     */
    public List<String> analizar(NodoArbol raiz) {
        tablaSimbolos.clear();
        ambitos.clear();
        pilaSemantica.clear();
        errores.clear();
        evaluaciones.clear();

        if (raiz == null) {
            return errores;
        }

        // Durante el análisis siempre existe el ámbito global.
        ambitos.add(new HashMap<String, Tipo>());

        // Una sola pasada: declaraciones primero, código después.
        for (NodoArbol hijo : raiz.getHijos()) {
            if (hijo.getNombre().startsWith("SECCION VARIABLES")) {
                analizarDeclaraciones(hijo);
            } else if (hijo.getNombre().startsWith("SECCION CODIGO")) {
                analizarCodigo(hijo);
            }
        }

        ambitos.clear();
        return errores;
    }

    /**
     * Expone la tabla de símbolos (variable -> tipo) que alimentó y consultó
     * el análisis, para mostrarla en la interfaz y en el archivo .tab.
     */
    public Map<String, Tipo> obtenerTablaSimbolos() {
        return new HashMap<String, Tipo>(tablaSimbolos);
    }

    /** Devuelve las expresiones constantes evaluadas durante el análisis. */
    public List<String> obtenerEvaluaciones() {
        return new ArrayList<String>(evaluaciones);
    }

    /** Traduce un tipo a su nombre en español, para los mensajes en pantalla. */
    public static String tipoEnEspanol(Tipo tipo) {
        switch (tipo) {
            case INT:
                return "entero";
            case CADENA:
                return "cadena";
            default:
                return "booleano";
        }
    }

    // =====================================================================
    // Sección de declaraciones (alimenta la tabla de símbolos)
    // =====================================================================

    private void analizarDeclaraciones(NodoArbol seccion) {
        for (NodoArbol decl : seccion.getHijos()) {
            if (!decl.getNombre().startsWith("Declaración:")) {
                continue;
            }

            // varent/varcad/varbool -> tipo correspondiente.
            String palabra = decl.getNombre()
                    .substring("Declaración:".length()).trim();
            Tipo tipo = tipoDePalabra(palabra);

            for (NodoArbol hijo : decl.getHijos()) {
                if (!hijo.getNombre().startsWith("Identificador:")) {
                    continue;
                }

                String nombreVar = hijo.getNombre()
                        .substring("Identificador:".length()).trim();

                // Doble declaración en un ámbito visible -> error.
                if (buscarTipo(nombreVar) != null) {
                    error(hijo, "La variable '" + nombreVar
                            + "' ya fue declarada");
                } else {
                    declararGlobal(nombreVar, tipo);
                }
            }
        }
    }

    // =====================================================================
    // Sección de código (usa la tabla y evalúa expresiones)
    // =====================================================================

    private void analizarCodigo(NodoArbol seccion) {
        for (NodoArbol instr : seccion.getHijos()) {
            analizarInstruccion(instr);
        }
    }

    /** Despacha cada tipo de instrucción a su comprobación semántica. */
    private void analizarInstruccion(NodoArbol instr) {
        String nombre = instr.getNombre();

        if (nombre.equals("Asignación")) {
            analizarAsignacion(instr);

        } else if (nombre.equals("Imprimir en consola")) {
            // ponerConsola acepta argumento de cualquier tipo.
            analizarImprimir(instr, null);

        } else if (nombre.startsWith("Imprimir entero:")) {
            // printInt exige una expresión entera.
            analizarImprimir(instr, Tipo.INT);

        } else if (nombre.startsWith("Imprimir booleano:")) {
            // printBool exige una expresión booleana.
            analizarImprimir(instr, Tipo.BOOL);

        } else if (nombre.startsWith("Lectura:")) {
            analizarLectura(instr);

        } else if (nombre.equals("Condicional SI")
                || nombre.equals("Bucle MIENTRAS")) {
            analizarCondicional(instr);
        }
    }

    /** Verifica la asignación: variable declarada y tipos compatibles. */
    private void analizarAsignacion(NodoArbol instr) {
        String nombreVar = null;
        NodoArbol nodoVariable = null;
        NodoArbol nodoExpr = null;

        for (NodoArbol hijo : instr.getHijos()) {
            if (hijo.getNombre().startsWith("Variable:")) {
                nombreVar = hijo.getNombre()
                        .substring("Variable:".length()).trim();
                nodoVariable = hijo;
            } else if (hijo.getNombre().equals("Expresión")) {
                nodoExpr = hijo;
            }
        }

        if (nombreVar == null || nodoExpr == null) {
            return;
        }

        // Uso de la tabla de símbolos: la variable debe existir en el ámbito.
        Tipo tipoVar = buscarTipo(nombreVar);

        if (tipoVar == null) {
            error(nodoVariable, "La variable '" + nombreVar
                    + "' no está declarada");
            return;
        }

        // El lado derecho se evalúa con la pila semántica.
        EntradaSemantica res = evaluarExpresion(nodoExpr);

        if (res != null && res.tipo != null && res.tipo != tipoVar) {
            error(instr, "Tipos incompatibles en asignación: la variable '"
                    + nombreVar + "' es " + tipoEnEspanol(tipoVar)
                    + " y la expresión es " + tipoEnEspanol(res.tipo));
            return;
        }

        if (res != null && res.constante && res.tipo != null) {
            evaluaciones.add("Línea " + instr.linea + ": " + nombreVar + " = "
                    + textoExpresion(nodoExpr) + " => " + mostrarValor(res)
                    + " (" + tipoEnEspanol(res.tipo) + ")");
        }
    }

    /**
     * Verifica el argumento de las funciones de escritura.
     *
     * @param exigido tipo exigido; null en ponerConsola (cualquier tipo)
     */
    private void analizarImprimir(NodoArbol instr, Tipo exigido) {
        String funcion = instr.getNombre().startsWith("Imprimir entero:")
                ? "printInt"
                : instr.getNombre().startsWith("Imprimir booleano:")
                ? "printBool"
                : "ponerConsola";

        for (NodoArbol hijo : instr.getHijos()) {
            if (!hijo.getNombre().equals("Expresión")) {
                continue;
            }

            EntradaSemantica res = evaluarExpresion(hijo);

            if (exigido != null && res != null && res.tipo != null
                    && res.tipo != exigido) {
                error(instr, funcion + " requiere una expresión de tipo "
                        + tipoEnEspanol(exigido));
                continue;
            }

            if (res != null && res.constante && res.tipo != null) {
                evaluaciones.add("Línea " + instr.linea + ": " + funcion + "("
                        + textoExpresion(hijo) + ") => " + mostrarValor(res)
                        + " (" + tipoEnEspanol(res.tipo) + ")");
            }
        }
    }

    /** Verifica que la lectura reciba una variable del tipo esperado. */
    private void analizarLectura(NodoArbol instr) {
        String palabra = instr.getNombre()
                .substring("Lectura:".length()).trim();
        Tipo esperado = tipoEsperadoLectura(palabra);

        for (NodoArbol hijo : instr.getHijos()) {
            if (!hijo.getNombre().startsWith("Variable:")) {
                continue;
            }

            String nombreVar = hijo.getNombre()
                    .substring("Variable:".length()).trim();
            Tipo tipoVar = buscarTipo(nombreVar);

            if (tipoVar == null) {
                error(hijo, "La variable '" + nombreVar
                        + "' no está declarada");
            } else if (tipoVar != esperado) {
                error(hijo, "La lectura " + palabra
                        + " requiere una variable de tipo "
                        + tipoEnEspanol(esperado) + ", pero '" + nombreVar
                        + "' es " + tipoEnEspanol(tipoVar));
            }
        }
    }

    /**
     * Verifica la condición de un SI o MIENTRAS y analiza su cuerpo con un
     * ámbito nuevo (alcance), que desaparece al terminar el bloque.
     */
    private void analizarCondicional(NodoArbol instr) {
        List<NodoArbol> expresiones = new ArrayList<>();
        String opRel = null;
        NodoArbol cuerpo = null;

        for (NodoArbol hijo : instr.getHijos()) {
            if (hijo.getNombre().equals("Expresión")) {
                expresiones.add(hijo);
            } else if (hijo.getNombre().startsWith("Operador relacional:")) {
                opRel = hijo.getNombre()
                        .substring("Operador relacional:".length()).trim();
            } else if (hijo.getNombre().startsWith("Cuerpo")) {
                cuerpo = hijo;
            }
        }

        NodoArbol exprIzq = !expresiones.isEmpty() ? expresiones.get(0) : null;
        NodoArbol exprDer = expresiones.size() > 1 ? expresiones.get(1) : null;

        if (exprIzq != null && exprDer != null && opRel != null) {
            // Evalúa ambos lados con la pila semántica y aplica el operador.
            EntradaSemantica izq = evaluarExpresion(exprIzq);
            EntradaSemantica der = evaluarExpresion(exprDer);
            EntradaSemantica cond = aplicarOperador(opRel, izq, der, instr);

            // Si la condición es constante, se resuelve en tiempo de análisis.
            if (cond != null && cond.constante && cond.tipo == Tipo.BOOL) {
                String inicio = instr.getNombre().startsWith("Bucle")
                        ? "while" : "si";
                evaluaciones.add("Línea " + instr.linea + ": " + inicio + " ("
                        + textoExpresion(exprIzq) + " " + opRel + " "
                        + textoExpresion(exprDer) + ") => "
                        + mostrarValor(cond) + " (booleano)");
            }
        }

        // El cuerpo abre un ámbito nuevo y vuelve al ámbito exterior al cerrarse.
        if (cuerpo != null) {
            entrarAmbito();
            for (NodoArbol ins : cuerpo.getHijos()) {
                analizarInstruccion(ins);
            }
            salirAmbito();
        }
    }

    // =====================================================================
    // Evaluación de expresiones con la pila semántica
    // =====================================================================

    /**
     * Evalúa una expresión con la PILA SEMÁNTICA (algoritmo de precedencia
     * con pila de operadores, estilo shunting-yard).
     *
     * Los operandos (valores o subexpresiones) se APILAN en la pila
     * semántica; los operadores se conservan en una pila auxiliar respetando
     * la prioridad (* y / mayor que + y -). Cada reducción desapila dos
     * operandos, aplica la operación y vuelve a apilar su resultado.
     *
     * Si ambos operandos son constantes, la operación se realiza en tiempo
     * de análisis (plegado de constantes).
     */
    private EntradaSemantica evaluarExpresion(NodoArbol exp) {
        if (exp == null || !exp.getNombre().equals("Expresión")) {
            return null;
        }

        // Pila de operadores pendientes; la pila semántica guarda los valores.
        Deque<String> operadores = new ArrayDeque<>();

        for (NodoArbol hijo : exp.getHijos()) {
            String nombre = hijo.getNombre();

            if (nombre.startsWith("Valor:")) {
                // Operando literal o variable -> se apila.
                pilaSemantica.push(tipoDeValor(hijo));

            } else if (nombre.equals("Expresión")) {
                // Subexpresión entre paréntesis: se evalúa y se vuelve a apilar.
                pilaSemantica.push(evaluarExpresion(hijo));

            } else if (nombre.startsWith("Operador:")) {
                String op = nombre.substring("Operador:".length()).trim();

                // Mientras el operador en la cima tenga mayor o igual
                // prioridad, se reduce antes de guardar el nuevo operador.
                while (!operadores.isEmpty()
                        && precedencia(operadores.peek()) >= precedencia(op)) {
                    reducir(operadores, exp);
                }

                operadores.push(op);
            }
        }

        // Reducciones finales: quedan menos operadores que operandos.
        while (!operadores.isEmpty()) {
            reducir(operadores, exp);
        }

        // Resultado de la expresión: la única entrada que queda encima.
        return pilaSemantica.isEmpty() ? null : pilaSemantica.pop();
    }

    /**
     * Un paso de reducción: desapila dos operandos y un operador, aplica la
     * operación y vuelve a apilar el resultado en la pila semántica.
     */
    private void reducir(Deque<String> operadores, NodoArbol exp) {
        String op = operadores.pop();
        EntradaSemantica der = pilaSemantica.isEmpty()
                ? null : pilaSemantica.pop();
        EntradaSemantica izq = pilaSemantica.isEmpty()
                ? null : pilaSemantica.pop();
        pilaSemantica.push(aplicarOperador(op, izq, der, exp));
    }

    /** Prioridad de los operadores aritméticos: * y / por encima de + y -. */
    private int precedencia(String op) {
        return (op.equals("*") || op.equals("/")) ? 2 : 1;
    }

    /**
     * Construye la entrada semántica de un valor: número entero, cadena o
     * variable. El tipo de las variables se obtiene de la tabla de símbolos.
     */
    private EntradaSemantica tipoDeValor(NodoArbol nodo) {
        EntradaSemantica entrada = new EntradaSemantica();
        String lexema = nodo.getNombre()
                .substring("Valor:".length()).trim();

        if (lexema.startsWith("\"")) {
            // Literal de cadena: tipo CADENA y valor constante (sin comillas).
            entrada.tipo = Tipo.CADENA;
            entrada.constante = true;
            entrada.valorCadena = lexema.substring(1, lexema.length() - 1);
            return entrada;
        }

        if (esNumero(lexema)) {
            // Literal numérico: tipo INT y valor constante.
            entrada.tipo = Tipo.INT;
            entrada.constante = true;
            entrada.valorInt = Integer.parseInt(lexema);
            return entrada;
        }

        // Uso de la tabla de símbolos: la variable debe existir en el ámbito.
        Tipo tipo = buscarTipo(lexema);

        if (tipo == null) {
            error(nodo, "La variable '" + lexema + "' no está declarada");
            return null;
        }

        entrada.tipo = tipo;
        entrada.constante = false;
        return entrada;
    }

    /**
     * Aplica un operador (aritmético o relacional) a dos operandos extraídos
     * de la pila semántica. Valida los tipos según la regla y, si ambos
     * operandos son constantes, calcula el resultado (plegado de constantes).
     */
    private EntradaSemantica aplicarOperador(String op, EntradaSemantica izq,
            EntradaSemantica der, NodoArbol nodo) {

        // Entrada de error: sin tipo válido y sin valor constante.
        EntradaSemantica invalida = new EntradaSemantica();

        if (izq == null || der == null || izq.tipo == null
                || der.tipo == null) {
            return invalida;
        }

        // ----- Operaciones aritméticas: solo enteros -----
        if (op.equals("+") || op.equals("-")
                || op.equals("*") || op.equals("/")) {

            if (izq.tipo != Tipo.INT || der.tipo != Tipo.INT) {
                error(nodo, "El operador '" + op
                        + "' requiere operandos de tipo entero");
                return invalida;
            }

            EntradaSemantica res = new EntradaSemantica();
            res.tipo = Tipo.INT;

            if (izq.constante && der.constante) {
                res.constante = true;

                switch (op) {
                    case "+":
                        res.valorInt = izq.valorInt + der.valorInt;
                        break;
                    case "-":
                        res.valorInt = izq.valorInt - der.valorInt;
                        break;
                    case "*":
                        res.valorInt = izq.valorInt * der.valorInt;
                        break;
                    default:
                        if (der.valorInt == 0) {
                            error(nodo, "División entre cero en la expresión");
                            return invalida;
                        }
                        res.valorInt = izq.valorInt / der.valorInt;
                        break;
                }
            }

            return res;
        }

        // ----- Operadores relacionales: el resultado siempre es booleano -----
        if (op.equals(">") || op.equals("<")) {
            if (izq.tipo != Tipo.INT || der.tipo != Tipo.INT) {
                error(nodo, "El operador relacional '" + op
                        + "' requiere operandos de tipo entero");
                return invalida;
            }
        } else if (izq.tipo != der.tipo) {
            error(nodo, "El operador relacional '" + op
                    + "' requiere operandos del mismo tipo");
            return invalida;
        }

        EntradaSemantica res = new EntradaSemantica();
        res.tipo = Tipo.BOOL;

        if (izq.constante && der.constante) {
            res.constante = true;

            if (izq.tipo == Tipo.INT) {
                int a = izq.valorInt;
                int b = der.valorInt;
                res.valorBool = op.equals("==") ? a == b
                        : op.equals("!=") ? a != b
                        : op.equals(">") ? a > b
                        : a < b;
            } else if (izq.tipo == Tipo.CADENA) {
                String a = izq.valorCadena;
                String b = der.valorCadena;
                res.valorBool = op.equals("==") ? a.equals(b) : !a.equals(b);
            }
        }

        return res;
    }

    // =====================================================================
    // Gestión del ámbito (alcance)
    // =====================================================================

    /** Abre un ámbito nuevo (cuerpo de SI / MIENTRAS). */
    private void entrarAmbito() {
        ambitos.add(new HashMap<String, Tipo>());
    }

    /** Cierra el ámbito actual y vuelve al ámbito inmediato anterior. */
    private void salirAmbito() {
        if (ambitos.size() > 1) {
            ambitos.remove(ambitos.size() - 1);
        }
    }

    /** Alimenta la tabla de símbolos con una declaración del ámbito global. */
    private void declararGlobal(String nombre, Tipo tipo) {
        ambitos.get(0).put(nombre, tipo);
        tablaSimbolos.put(nombre, tipo);
    }

    /**
     * Consulta la tabla de símbolos recorriendo los ámbitos del más interno
     * al más externo hasta encontrar la variable.
     */
    private Tipo buscarTipo(String nombre) {
        for (int i = ambitos.size() - 1; i >= 0; i--) {
            Map<String, Tipo> ambito = ambitos.get(i);

            if (ambito.containsKey(nombre)) {
                return ambito.get(nombre);
            }
        }

        return null;
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    private boolean esNumero(String s) {
        if (s.isEmpty()) {
            return false;
        }

        int i = 0;

        if (s.charAt(0) == '-') {
            i = 1;
        }

        if (i >= s.length()) {
            return false;
        }

        for (; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    private Tipo tipoDePalabra(String palabra) {
        switch (palabra) {
            case "varent":
                return Tipo.INT;
            case "varcad":
                return Tipo.CADENA;
            case "varbool":
                return Tipo.BOOL;
            default:
                return null;
        }
    }

    private Tipo tipoEsperadoLectura(String palabra) {
        switch (palabra) {
            case "leerent":
                return Tipo.INT;
            case "leercad":
                return Tipo.CADENA;
            default:
                return Tipo.BOOL;
        }
    }

    private void error(NodoArbol nodo, String mensaje) {
        errores.add("Renglón: " + nodo.linea + ", Columna: " + nodo.columna
                + ", Error semántico: " + mensaje);
    }

    /** Reconstruye el texto de una expresión a partir del árbol. */
    private String textoExpresion(NodoArbol exp) {
        StringBuilder sb = new StringBuilder();

        for (NodoArbol hijo : exp.getHijos()) {
            String nombre = hijo.getNombre();

            if (nombre.startsWith("Valor:")) {
                sb.append(nombre.substring("Valor:".length()).trim());
            } else if (nombre.startsWith("Operador:")) {
                sb.append(' ')
                        .append(nombre.substring("Operador:".length()).trim())
                        .append(' ');
            } else if (nombre.equals("Expresión")) {
                sb.append('(').append(textoExpresion(hijo)).append(')');
            }
        }

        return sb.toString().trim();
    }

    /** Muestra el valor de una entrada constante de la pila semántica. */
    private String mostrarValor(EntradaSemantica entrada) {
        if (entrada.tipo == Tipo.INT) {
            return String.valueOf(entrada.valorInt);
        }

        if (entrada.tipo == Tipo.CADENA) {
            return entrada.valorCadena;
        }

        return String.valueOf(entrada.valorBool);
    }
}