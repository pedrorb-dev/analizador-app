public class TokenData {

    public String lexema;
    public Tokens token;
    public String nombreToken;
    public int linea;
    public int columna;
    public int ref;

    public TokenData(String lexema, Tokens token, String nombreToken,
            int linea, int columna, int ref) {
        this.lexema = lexema;
        this.token = token;
        this.nombreToken = nombreToken;
        this.linea = linea;
        this.columna = columna;
        this.ref = ref;
    }
}