import java.util.ArrayList;
import java.util.List;

public class NodoArbol {

    private String nombre;
    private List<NodoArbol> hijos;
    public int linea;
    public int columna;

    public NodoArbol(String nombre) {
        this.nombre = nombre;
        this.hijos = new ArrayList<>();
        this.linea = 0;
        this.columna = 0;
    }

    public NodoArbol(String nombre, int linea) {
        this.nombre = nombre;
        this.hijos = new ArrayList<>();
        this.linea = linea;
        this.columna = 0;
    }

    public NodoArbol(String nombre, int linea, int columna) {
        this.nombre = nombre;
        this.hijos = new ArrayList<>();
        this.linea = linea;
        this.columna = columna;
    }

    public void agregarHijo(NodoArbol hijo) {
        hijos.add(hijo);
    }

    public String getNombre() {
        return nombre;
    }

    public List<NodoArbol> getHijos() {
        return hijos;
    }

    public String imprimir(String espacio) {
        StringBuilder sb = new StringBuilder();
        sb.append(espacio).append(nombre).append("\n");

        for (NodoArbol hijo : hijos) {
            sb.append(hijo.imprimir(espacio + "   "));
        }

        return sb.toString();
    }
}