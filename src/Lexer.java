import java.io.*;

public class Lexer {

    private String contenido;
    private int pos = 0;

    public int yyline = 0;
    public String Lexema = "";

    public Lexer(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;

        while ((c = reader.read()) != -1) {
            sb.append((char) c);
        }

        contenido = sb.toString();
    }

    public Tokens yylex() {

        while (pos < contenido.length()) {

            char c = contenido.charAt(pos);

            // Saltos de línea
            if (c == '\n') {
                yyline++;
                pos++;
                continue;
            }

            // Espacios
            if (Character.isWhitespace(c)) {
                pos++;
                continue;
            }

            // Comentario de una línea
            if (c == '/' && pos + 1 < contenido.length()
                    && contenido.charAt(pos + 1) == '/') {

                while (pos < contenido.length()
                        && contenido.charAt(pos) != '\n') {
                    pos++;
                }

                continue;
            }

            // Comentario multilínea
            if (c == '/' && pos + 1 < contenido.length()
                    && contenido.charAt(pos + 1) == '*') {

                pos += 2;

                while (pos + 1 < contenido.length()
                        && !(contenido.charAt(pos) == '*'
                        && contenido.charAt(pos + 1) == '/')) {

                    if (contenido.charAt(pos) == '\n') {
                        yyline++;
                    }

                    pos++;
                }

                if (pos + 1 < contenido.length()) {
                    pos += 2;
                }

                continue;
            }

            // Palabra reservada especial p#
            if (c == 'p' && pos + 1 < contenido.length()
                    && contenido.charAt(pos + 1) == '#') {

                Lexema = "p#";
                pos += 2;
                return Tokens.Reservadas;
            }

            // Cadenas
            if (c == '"') {
                return cadena();
            }

            // Números
            if (Character.isDigit(c)
                    || (c == '-' && pos + 1 < contenido.length()
                    && Character.isDigit(contenido.charAt(pos + 1)))) {

                return numero();
            }

            // Identificadores y palabras reservadas
            if (Character.isLetter(c)) {
                return palabra();
            }

            // Operadores y símbolos
            switch (c) {

                case '=':
                    if (siguiente('=')) {
                        return tokenDoble(Tokens.IgualIgual);
                    }
                    return tokenSimple(Tokens.Igual);

                case '!':
                    if (siguiente('=')) {
                        return tokenDoble(Tokens.Diferente);
                    }

                    Lexema = String.valueOf(c);
                    pos++;
                    return Tokens.ERROR;

                case '+':
                    return tokenSimple(Tokens.Suma);

                case '-':
                    return tokenSimple(Tokens.Resta);

                case '*':
                    return tokenSimple(Tokens.Multiplicacion);

                case '/':
                    return tokenSimple(Tokens.Division);

                case '>':
                    return tokenSimple(Tokens.Mayor);

                case '<':
                    return tokenSimple(Tokens.Menor);

                case '(':
                    return tokenSimple(Tokens.ParentesisA);

                case ')':
                    return tokenSimple(Tokens.ParentesisC);

                case '{':
                    return tokenSimple(Tokens.LlaveA);

                case '}':
                    return tokenSimple(Tokens.LlaveC);

                case ';':
                    return tokenSimple(Tokens.PC);

                default:
                    Lexema = String.valueOf(c);
                    pos++;
                    return Tokens.ERROR;
            }
        }

        return null;
    }

    private Tokens palabra() {

        int inicio = pos;

        while (pos < contenido.length()
                && Character.isLetterOrDigit(contenido.charAt(pos))) {
            pos++;
        }

        Lexema = contenido.substring(inicio, pos);

        if (esReservada(Lexema)) {
            return Tokens.Reservadas;
        }

        return Tokens.Identificador;
    }

    private Tokens numero() {

        int inicio = pos;

        if (contenido.charAt(pos) == '-') {
            pos++;
        }

        while (pos < contenido.length()
                && Character.isDigit(contenido.charAt(pos))) {
            pos++;
        }

        if (pos < contenido.length()
                && Character.isLetter(contenido.charAt(pos))) {

            while (pos < contenido.length()
                    && Character.isLetterOrDigit(contenido.charAt(pos))) {
                pos++;
            }

            Lexema = contenido.substring(inicio, pos);
            return Tokens.ERROR;
        }

        Lexema = contenido.substring(inicio, pos);
        return Tokens.Numero;
    }

    private Tokens cadena() {

        int inicio = pos;
        pos++;

        while (pos < contenido.length()
                && contenido.charAt(pos) != '"') {

            if (contenido.charAt(pos) == '\n') {
                yyline++;
                Lexema = contenido.substring(inicio, pos);
                return Tokens.ERROR;
            }

            pos++;
        }

        if (pos < contenido.length()
                && contenido.charAt(pos) == '"') {

            pos++;
            Lexema = contenido.substring(inicio, pos);
            return Tokens.Cadena;
        }

        Lexema = contenido.substring(inicio, pos);
        return Tokens.ERROR;
    }

    private boolean esReservada(String palabra) {

        return palabra.equals("varent")
                || palabra.equals("varcad")
                || palabra.equals("varbool")
                || palabra.equals("ponerConsola")
                || palabra.equals("leerent")
                || palabra.equals("leercad")
                || palabra.equals("leerbol")
                || palabra.equals("si")
                || palabra.equals("entonces")
                || palabra.equals("fin")
                || palabra.equals("variables")
                || palabra.equals("codigo");
    }

    private boolean siguiente(char esperado) {

        return pos + 1 < contenido.length()
                && contenido.charAt(pos + 1) == esperado;
    }

    private Tokens tokenSimple(Tokens token) {

        Lexema = String.valueOf(contenido.charAt(pos));
        pos++;
        return token;
    }

    private Tokens tokenDoble(Tokens token) {

        Lexema = contenido.substring(pos, pos + 2);
        pos += 2;
        return token;
    }
}