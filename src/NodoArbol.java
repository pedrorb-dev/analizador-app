import java.util.ArrayList;
import java.util.List;

public class NodoArbol {

    private String nombre;
    private List<NodoArbol> hijos;

    public NodoArbol(String nombre) {
        this.nombre = nombre;
        this.hijos = new ArrayList<>();
    }

    public void agregarHijo(NodoArbol hijo) {
        hijos.add(hijo);
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