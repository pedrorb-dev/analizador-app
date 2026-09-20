/*
 * Fecha: 30/04/2026
 * Descripción: Interfaz gráfica para el Analizador Léxico y Sintáctico
 * Genera archivos: .tok, .tab, .dep, .arb
 */

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class FramePrincipal extends JFrame {

    class EntradaTabla {

        String lexema;
        String tipoToken;
        int ref;

        EntradaTabla(String lexema, String tipoToken, int ref) {
            this.lexema = lexema;
            this.tipoToken = tipoToken;
            this.ref = ref;
        }
    }

    private Map<String, Integer> mapaRef = new HashMap<>();

    private int contadorReservada = 100;
    private int contadorIdentificador = 200;
    private int contadorNumero = 300;
    private int contadorCadena = 400;
    private int contadorError = 1900;

    private Stack<Integer> pilaParentesis = new Stack<>();
    private boolean cadenaAbierta = false;
    private int ultimaLineaProcesada = 1;
    private int ultimaColumnaProcesada = 1;
    private boolean ultimoTokenFuePuntoComa = true;
    private String ultimoLexema = "";
    private String ultimoTokenNombre = "";

    private Set<String> tokensSinPuntoComa =
            new HashSet<>(Arrays.asList(
                    "p#",
                    "variables",
                    "codigo",
                    "fin",
                    "si",
                    "entonces",
                    "while"
            ));

    public FramePrincipal() {

        setTitle("Analizador Léxico y Sintáctico");
        setSize(850, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel panelSuperior = new JPanel(new BorderLayout(5, 5));

        JTextField txtRutaArchivo = new JTextField();
        txtRutaArchivo.setEditable(false);

        JButton btnAnalizar = new JButton("Analizar");

        panelSuperior.add(txtRutaArchivo, BorderLayout.CENTER);
        panelSuperior.add(btnAnalizar, BorderLayout.EAST);

        JTextArea txtaSalida = new JTextArea();
        txtaSalida.setEditable(false);

        JScrollPane scroll = new JScrollPane(txtaSalida);

        panel.add(panelSuperior, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        btnAnalizar.addActionListener(e -> analizarArchivo(txtRutaArchivo, txtaSalida));

        add(panel);
    }

    private void analizarArchivo(JTextField txtRutaArchivo, JTextArea txtaSalida) {

        reiniciarDatos();

        JFileChooser eleccion = new JFileChooser();
        int opcion = eleccion.showOpenDialog(null);

        if (opcion != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {

            File archivo = eleccion.getSelectedFile();

            String rutaBase = archivo.getParent();

            String nombre = archivo.getName()
                    .replaceFirst("[.][^.]+$", "");

            txtRutaArchivo.setText(archivo.getAbsolutePath());

            String contenido = new String(
                    Files.readAllBytes(archivo.toPath())
            );

            archivoDep(contenido, rutaBase, nombre);

            Lexer lexer = new Lexer(new StringReader(contenido));

            StringBuilder resultadoTok = new StringBuilder();
            StringBuilder erroresEncontrados = new StringBuilder();

            java.util.List<EntradaTabla> tablaSimbolos = new ArrayList<>();
            java.util.List<TokenData> listaTokens = new ArrayList<>();

            while (true) {

                Tokens token = lexer.yylex();

                if (token == null) {
                    break;
                }

                int lineaActual = lexer.yyline + 1;
                int columnaActual = lexer.ultimaColumna;
                String lexemaActual = lexer.Lexema;
                String nombreToken = obtenerNombreToken(token);

                if (lexemaActual == null) {
                    lexemaActual = "";
                }

                // Validación de punto y coma
                if (lineaActual > ultimaLineaProcesada) {

                    boolean requierePuntoComa =
                            !tokensSinPuntoComa.contains(ultimoLexema)
                                    && !ultimoTokenNombre.equals("LlaveA")
                                    && !ultimoTokenNombre.equals("LlaveC")
                                    && !ultimoLexema.equals("{")
                                    && !ultimoLexema.equals("");

                    if (!ultimoTokenFuePuntoComa && requierePuntoComa) {
                        erroresEncontrados.append(
                                "Renglón: "
                                        + ultimaLineaProcesada
                                        + ", Columna: "
                                        + ultimaColumnaProcesada
                                        + ", Error: Falta un punto y coma (;)\n"
                        );
                    }

                    ultimaLineaProcesada = lineaActual;
                    ultimaColumnaProcesada = columnaActual;
                }

                ultimoTokenFuePuntoComa = (token == Tokens.PC);
                ultimoLexema = lexemaActual;
                ultimoTokenNombre = nombreToken;

                // Validación de paréntesis
                if (token == Tokens.ParentesisA) {
                    pilaParentesis.push(lineaActual);

                } else if (token == Tokens.ParentesisC) {

                    if (!pilaParentesis.isEmpty()) {
                        pilaParentesis.pop();
                    } else {
                        erroresEncontrados.append(
                                "Renglón: "
                                        + lineaActual
                                        + ", Columna: "
                                        + columnaActual
                                        + ", Error: Paréntesis ')' sin apertura\n"
                        );
                    }
                }

                // Error léxico
                if (token == Tokens.ERROR) {
                    erroresEncontrados.append(
                            "Renglón: "
                                    + lineaActual
                                    + ", Columna: "
                                    + columnaActual
                                    + ", Error léxico en: "
                                    + lexemaActual
                                    + "\n"
                    );
                }

                // Validación de comillas
                if (lexemaActual.startsWith("\"")
                        && !lexemaActual.endsWith("\"")) {
                    cadenaAbierta = true;

                } else if (lexemaActual.startsWith("\"")
                        && lexemaActual.endsWith("\"")) {
                    cadenaAbierta = false;
                }

                int ref = obtenerRef(lexemaActual, token);

                resultadoTok.append(
                        "Renglón: "
                                + lineaActual
                                + ", Columna: "
                                + columnaActual
                                + ", Lexema: "
                                + lexemaActual
                                + ", Token: "
                                + nombreToken
                                + ", REF: "
                                + ref
                                + "\n"
                );

                listaTokens.add(
                        new TokenData(
                                lexemaActual,
                                token,
                                nombreToken,
                                lineaActual,
                                columnaActual,
                                ref
                        )
                );

                if (ref != -1) {
                    tablaSimbolos.add(
                            new EntradaTabla(
                                    lexemaActual,
                                    nombreToken,
                                    ref
                            )
                    );
                }
            }

            if (!pilaParentesis.isEmpty()) {
                erroresEncontrados.append(
                        "Error: Falta cerrar un paréntesis\n"
                );
            }

            if (cadenaAbierta) {
                erroresEncontrados.append(
                        "Error: Falta cerrar comillas\n"
                );
            }

            if (erroresEncontrados.length() > 0) {
                resultadoTok.append(
                        "\n========== ERRORES ENCONTRADOS ==========\n"
                );
                resultadoTok.append(erroresEncontrados);
            }

            String resultadoSintactico;
            String resultadoSemantico = "";
            String arbol = "";
            String anexoSemantico = "";

            try {

                Parser parser = new Parser(listaTokens);
                parser.programa();

                arbol = parser.obtenerArbol();

                AnalizadorSemantico semantico = new AnalizadorSemantico();
                java.util.List<String> erroresSemanticos =
                        semantico.analizar(parser.obtenerRaiz());

                StringBuilder resultadoSem = new StringBuilder();

                resultadoSem.append("\n\n========== ANALISIS SEMANTICO ==========\n");

                if (erroresSemanticos.isEmpty()) {
                    resultadoSem.append("Análisis semántico correcto.\n");

                    // Tabla de símbolos que alimentó y consultó el análisis.
                    java.util.Map<String, AnalizadorSemantico.Tipo> tablaSem =
                            semantico.obtenerTablaSimbolos();

                    resultadoSem.append(
                            "\n--- TABLA DE SÍMBOLOS (variable -> tipo) ---\n"
                    );

                    StringBuilder anexoTabla = new StringBuilder();

                    if (tablaSem.isEmpty()) {
                        resultadoSem.append(
                                "(el programa no declara variables)\n"
                        );
                    } else {
                        for (java.util.Map.Entry<String, AnalizadorSemantico.Tipo> e
                                : tablaSem.entrySet()) {

                            resultadoSem.append("   ")
                                    .append(e.getKey())
                                    .append(" -> ")
                                    .append(AnalizadorSemantico
                                            .tipoEnEspanol(e.getValue()))
                                    .append("\n");

                            anexoTabla.append(e.getKey())
                                    .append(" | ")
                                    .append(AnalizadorSemantico
                                            .tipoEnEspanol(e.getValue()))
                                    .append("\n");
                        }
                    }

                    anexoSemantico = anexoTabla.toString();

                    // Expresiones evaluadas por plegado de constantes.
                    resultadoSem.append(
                            "\n--- EVALUACIÓN DE EXPRESIONES (plegado de constantes) ---\n"
                    );

                    java.util.List<String> evaluaciones =
                            semantico.obtenerEvaluaciones();

                    if (evaluaciones.isEmpty()) {
                        resultadoSem.append(
                                "(no hay expresiones constantes que evaluar)\n"
                        );
                    } else {
                        for (String ev : evaluaciones) {
                            resultadoSem.append("   ").append(ev).append("\n");
                        }
                    }
                } else {
                    for (String e : erroresSemanticos) {
                        resultadoSem.append(e).append("\n");
                    }
                }

                resultadoSemantico = resultadoSem.toString();

                resultadoSintactico =
                        "\n\n========== ANALISIS SINTACTICO ==========\n"
                                + "Análisis sintáctico correcto.\n"
                                + "Árbol generado correctamente en el archivo .arb\n";

            } catch (Exception ex) {

                resultadoSintactico =
                        "\n\n========== ANALISIS SINTACTICO ==========\n"
                                + ex.getMessage()
                                + "\n";

                arbol = "No se generó árbol porque existen errores sintácticos.\n"
                        + ex.getMessage();
            }

            txtaSalida.setText(
                    resultadoTok.toString()
                            + resultadoSemantico
                            + resultadoSintactico
            );

            archivoTok(resultadoTok.toString(), rutaBase, nombre);
            archivoTab(tablaSimbolos, rutaBase, nombre, anexoSemantico);
            archivoArbol(arbol, rutaBase, nombre);

            JOptionPane.showMessageDialog(
                    null,
                    "Archivos generados correctamente:\n"
                            + nombre + ".tok\n"
                            + nombre + ".tab\n"
                            + nombre + ".dep\n"
                            + nombre + ".arb"
            );

        } catch (Exception ex) {

            ex.printStackTrace();

            JOptionPane.showMessageDialog(
                    null,
                    "Error: " + ex.getMessage()
            );
        }
    }

    private void reiniciarDatos() {

        contadorReservada = 100;
        contadorIdentificador = 200;
        contadorNumero = 300;
        contadorCadena = 400;
        contadorError = 1900;

        mapaRef.clear();
        pilaParentesis.clear();

        cadenaAbierta = false;
        ultimaLineaProcesada = 1;
        ultimaColumnaProcesada = 1;
        ultimoTokenFuePuntoComa = true;
        ultimoLexema = "";
        ultimoTokenNombre = "";
    }

    private int obtenerRef(String lexema, Tokens token) {

        if (mapaRef.containsKey(lexema)) {
            return mapaRef.get(lexema);
        }

        int ref = -1;

        switch (token) {

            case Reservadas:
                ref = contadorReservada++;
                break;

            case Identificador:
                ref = contadorIdentificador++;
                break;

            case Numero:
                ref = contadorNumero++;
                break;

            case Cadena:
                ref = contadorCadena++;
                break;

            case ERROR:
                ref = contadorError++;
                break;

            default:
                ref = -1;
        }

        if (ref != -1) {
            mapaRef.put(lexema, ref);
        }

        return ref;
    }

    private String obtenerNombreToken(Tokens token) {
        return token.name();
    }

    private void archivoTok(String contenido, String ruta, String nombre) {

        try (PrintWriter escribir =
                     new PrintWriter(
                             new FileWriter(
                                     ruta + File.separator + nombre + ".tok"
                             )
                     )) {

            escribir.print(contenido);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al guardar .tok");
        }
    }

    private void archivoTab(
            java.util.List<EntradaTabla> tabla,
            String ruta,
            String nombre,
            String anexoSemantico
    ) {

        try (PrintWriter escribir =
                     new PrintWriter(
                             new FileWriter(
                                     ruta + File.separator + nombre + ".tab"
                             )
                     )) {

            escribir.println("No | LEXEMA | TOKEN | REF");

            int num = 1;

            for (EntradaTabla e : tabla) {

                escribir.println(
                        num
                                + " | "
                                + e.lexema
                                + " | "
                                + e.tipoToken
                                + " | "
                                + e.ref
                );

                num++;
            }

            if (!anexoSemantico.isEmpty()) {
                escribir.println(
                        "\n--- TABLA DE SÍMBOLOS DEL ANÁLISIS SEMÁNTICO ---"
                );
                escribir.println("LEXEMA | TIPO");
                escribir.print(anexoSemantico);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al guardar .tab");
        }
    }

    private void archivoDep(String contenido, String ruta, String nombre) {

        try {

            String limpio = contenido.replaceAll("\\s+", "");

            try (PrintWriter escribir =
                         new PrintWriter(
                                 new FileWriter(
                                         ruta + File.separator + nombre + ".dep"
                                 )
                         )) {

                escribir.print(limpio);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al guardar .dep");
        }
    }

    private void archivoArbol(String contenido, String ruta, String nombre) {

    try (PrintWriter escribir = new PrintWriter(
            new FileWriter(ruta + File.separator + nombre + ".arb"))) {

        escribir.println("==============================================");
        escribir.println("        ÁRBOL DE ANÁLISIS SINTÁCTICO");
        escribir.println("==============================================");
        escribir.println("Archivo analizado: " + nombre);
        escribir.println("----------------------------------------------");
        escribir.println(contenido);

    } catch (Exception e) {
        JOptionPane.showMessageDialog(null, "Error al guardar .arb");
    }
}

    public static void main(String[] args) {
        new FramePrincipal().setVisible(true);
    }
}