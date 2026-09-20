import java.util.List;

public class Parser {

    private List<TokenData> tokens;
    private int posicion = 0;
    private TokenData actual;
    private NodoArbol raiz;

    public Parser(List<TokenData> tokens) {
        this.tokens = tokens;

        if (!tokens.isEmpty()) {
            actual = tokens.get(0);
        } else {
            actual = null;
        }
    }

    private void avanzar() {
        posicion++;

        if (posicion < tokens.size()) {
            actual = tokens.get(posicion);
        } else {
            actual = null;
        }
    }

    private void error(String mensaje) throws Exception {
        int linea = actual != null ? actual.linea : -1;
        int columna = actual != null ? actual.columna : -1;
        throw new Exception("Renglón: " + linea + ", Columna: " + columna
                + ", Error sintáctico: " + mensaje);
    }

    private NodoArbol nuevoNodo(String nombre) {
        return new NodoArbol(nombre,
                actual != null ? actual.linea : 0,
                actual != null ? actual.columna : 0);
    }

    private boolean esReservada(String palabra) {
        return actual != null
                && actual.token == Tokens.Reservadas
                && actual.lexema.equals(palabra);
    }

    private void coincidir(Tokens esperado) throws Exception {
        if (actual != null && actual.token == esperado) {
            avanzar();
        } else {
            error("Se esperaba " + esperado + " y se encontró " + (actual != null ? actual.token : "FIN"));
        }
    }

    public void programa() throws Exception {

        raiz = nuevoNodo("PROGRAMA");

        if (!esReservada("p#")) {
            error("El programa debe iniciar con p#");
        }

        raiz.agregarHijo(nuevoNodo("Inicio: p#"));
        avanzar();

        if (!esReservada("variables")) {
            error("Se esperaba la sección variables");
        }

        NodoArbol nodoVariables = nuevoNodo("SECCION VARIABLES");
        raiz.agregarHijo(nodoVariables);
        avanzar();

        declaraciones(nodoVariables);

        if (!esReservada("codigo")) {
            error("Se esperaba la sección codigo");
        }

        NodoArbol nodoCodigo = nuevoNodo("SECCION CODIGO");
        raiz.agregarHijo(nodoCodigo);
        avanzar();

        instrucciones(nodoCodigo);

        if (!esReservada("fin")) {
            error("Se esperaba fin");
        }

        raiz.agregarHijo(nuevoNodo("Fin del programa"));
        avanzar();

        if (actual != null) {
            error("Hay contenido después de fin");
        }
    }

    private void declaraciones(NodoArbol padre) throws Exception {

        while (actual != null
                && actual.token == Tokens.Reservadas
                && (actual.lexema.equals("varent")
                || actual.lexema.equals("varcad")
                || actual.lexema.equals("varbool"))) {

            NodoArbol declaracion = nuevoNodo("Declaración: " + actual.lexema);
            avanzar();

            if (actual != null && actual.token == Tokens.Identificador) {
                declaracion.agregarHijo(nuevoNodo("Identificador: " + actual.lexema));
                avanzar();
            } else {
                error("Se esperaba un identificador");
            }

            coincidir(Tokens.PC);
            padre.agregarHijo(declaracion);
        }
    }

    private void instrucciones(NodoArbol padre) throws Exception {

        while (actual != null && !esReservada("fin")) {
            instruccion(padre);
        }
    }

    private void instruccion(NodoArbol padre) throws Exception {

        if (actual.token == Tokens.Identificador) {
            asignacion(padre);

        } else if (esReservada("ponerConsola")) {
            imprimir(padre);

        } else if (esReservada("printInt")) {
            imprimirEntero(padre);

        } else if (esReservada("printBool")) {
            imprimirBoolean(padre);

        } else if (esReservada("leerent")
                || esReservada("leercad")
                || esReservada("leerbol")) {
            lectura(padre);

        } else if (esReservada("si")) {
            condicional(padre);

        } else if (esReservada("while")) {
            bucle(padre);

        } else {
            error("Instrucción no válida");
        }
    }

    private void asignacion(NodoArbol padre) throws Exception {

        NodoArbol nodo = nuevoNodo("Asignación");
        nodo.agregarHijo(nuevoNodo("Variable: " + actual.lexema));

        coincidir(Tokens.Identificador);
        coincidir(Tokens.Igual);

        expresion(nodo);

        coincidir(Tokens.PC);
        padre.agregarHijo(nodo);
    }

    private void imprimir(NodoArbol padre) throws Exception {

        NodoArbol nodo = nuevoNodo("Imprimir en consola");

        avanzar();
        coincidir(Tokens.ParentesisA);
        expresion(nodo);
        coincidir(Tokens.ParentesisC);
        coincidir(Tokens.PC);

        padre.agregarHijo(nodo);
    }

    private void imprimirEntero(NodoArbol padre) throws Exception {

        NodoArbol nodo = nuevoNodo("Imprimir entero: printInt");

        avanzar();
        coincidir(Tokens.ParentesisA);
        expresion(nodo);
        coincidir(Tokens.ParentesisC);
        coincidir(Tokens.PC);

        padre.agregarHijo(nodo);
    }

    private void imprimirBoolean(NodoArbol padre) throws Exception {

        NodoArbol nodo = nuevoNodo("Imprimir booleano: printBool");

        avanzar();
        coincidir(Tokens.ParentesisA);
        expresion(nodo);
        coincidir(Tokens.ParentesisC);
        coincidir(Tokens.PC);

        padre.agregarHijo(nodo);
    }

    private void lectura(NodoArbol padre) throws Exception {

        NodoArbol nodo = nuevoNodo("Lectura: " + actual.lexema);

        avanzar();
        coincidir(Tokens.ParentesisA);

        if (actual != null && actual.token == Tokens.Identificador) {
            nodo.agregarHijo(nuevoNodo("Variable: " + actual.lexema));
            avanzar();
        } else {
            error("Se esperaba identificador en lectura");
        }

        coincidir(Tokens.ParentesisC);
        coincidir(Tokens.PC);

        padre.agregarHijo(nodo);
    }

    private void condicional(NodoArbol padre) throws Exception {

        NodoArbol nodo = nuevoNodo("Condicional SI");

        avanzar();
        coincidir(Tokens.ParentesisA);

        expresion(nodo);
        operadorRelacional(nodo);
        expresion(nodo);

        coincidir(Tokens.ParentesisC);

        if (!esReservada("entonces")) {
            error("Se esperaba entonces");
        }

        avanzar();

        NodoArbol cuerpo = nuevoNodo("Cuerpo del SI");

        while (actual != null && !esReservada("fin")) {
            instruccion(cuerpo);
        }

        if (!esReservada("fin")) {
            error("Se esperaba fin del condicional");
        }

        avanzar();

        nodo.agregarHijo(cuerpo);
        padre.agregarHijo(nodo);
    }

    private void bucle(NodoArbol padre) throws Exception {

        NodoArbol nodo = nuevoNodo("Bucle MIENTRAS");

        avanzar();
        coincidir(Tokens.ParentesisA);

        expresion(nodo);
        operadorRelacional(nodo);
        expresion(nodo);

        coincidir(Tokens.ParentesisC);

        NodoArbol cuerpo = nuevoNodo("Cuerpo del MIENTRAS");

        while (actual != null && !esReservada("fin")) {
            instruccion(cuerpo);
        }

        if (!esReservada("fin")) {
            error("Se esperaba fin del bucle");
        }

        avanzar();

        nodo.agregarHijo(cuerpo);
        padre.agregarHijo(nodo);
    }

    private void expresion(NodoArbol padre) throws Exception {

        NodoArbol nodoExpresion = nuevoNodo("Expresión");

        termino(nodoExpresion);

        while (actual != null
                && (actual.token == Tokens.Suma || actual.token == Tokens.Resta)) {

            nodoExpresion.agregarHijo(nuevoNodo("Operador: " + actual.lexema));
            avanzar();
            termino(nodoExpresion);
        }

        padre.agregarHijo(nodoExpresion);
    }

    private void termino(NodoArbol padre) throws Exception {

        factor(padre);

        while (actual != null
                && (actual.token == Tokens.Multiplicacion || actual.token == Tokens.Division)) {

            padre.agregarHijo(nuevoNodo("Operador: " + actual.lexema));
            avanzar();
            factor(padre);
        }
    }

    private void factor(NodoArbol padre) throws Exception {

        if (actual == null) {
            error("Expresión incompleta");
        }

        if (actual.token == Tokens.Identificador
                || actual.token == Tokens.Numero
                || actual.token == Tokens.Cadena) {

            padre.agregarHijo(nuevoNodo("Valor: " + actual.lexema));
            avanzar();

        } else if (actual.token == Tokens.ParentesisA) {

            avanzar();
            expresion(padre);
            coincidir(Tokens.ParentesisC);

        } else {
            error("Expresión no válida");
        }
    }

    private void operadorRelacional(NodoArbol padre) throws Exception {

        if (actual != null
                && (actual.token == Tokens.IgualIgual
                || actual.token == Tokens.Diferente
                || actual.token == Tokens.Mayor
                || actual.token == Tokens.Menor)) {

            padre.agregarHijo(nuevoNodo("Operador relacional: " + actual.lexema));
            avanzar();

        } else {
            error("Se esperaba operador relacional");
        }
    }

    public String obtenerArbol() {
        if (raiz == null) {
            return "No se generó árbol sintáctico.";
        }

        return raiz.imprimir("");
    }

    public NodoArbol obtenerRaiz() {
        return raiz;
    }
}