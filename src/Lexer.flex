import static Tokens.*;

%%
%class Lexer
%type Tokens
%line
%column

L=[a-zA-Z]
D=[0-9]
espacio=[ \t\r\n]+

%{
    public String Lexema;
%}

%%

// PALABRAS RESERVADAS
("p#"|varent|varcad|varbool|ponerConsola|printInt|printBool|leerent|leercad|leerbol|si|entonces|while|fin|variables|codigo) {
    Lexema = yytext();
    return Tokens.Reservadas;
}

// ESPACIOS
{espacio} { /* ignorar */ }

// COMENTARIOS
"//".* { /* ignorar */ }
"/*" ~"*/" { /* ignorar */ }

// OPERADORES
"==" { Lexema = yytext(); return Tokens.IgualIgual; }
"!=" { Lexema = yytext(); return Tokens.Diferente; }
"="  { Lexema = yytext(); return Tokens.Igual; }
"+"  { Lexema = yytext(); return Tokens.Suma; }
"-"  { Lexema = yytext(); return Tokens.Resta; }
"*"  { Lexema = yytext(); return Tokens.Multiplicacion; }
"/"  { Lexema = yytext(); return Tokens.Division; }
">"  { Lexema = yytext(); return Tokens.Mayor; }
"<"  { Lexema = yytext(); return Tokens.Menor; }

// PARÉNTESIS
"(" { Lexema = yytext(); return Tokens.ParentesisA; }
")" { Lexema = yytext(); return Tokens.ParentesisC; }

// LLAVES
"{" { Lexema = yytext(); return Tokens.LlaveA; }
"}" { Lexema = yytext(); return Tokens.LlaveC; }

// PUNTO Y COMA
";" { Lexema = yytext(); return Tokens.PC; }

// CADENA VÁLIDA
\"([^\"\\]|\\.)*\" {
    Lexema = yytext();
    return Tokens.Cadena;
}

// CADENA NO CERRADA
\"[^\"]* {
    Lexema = yytext();
    return Tokens.ERROR;
}

// NÚMEROS
-?{D}+ {
    Lexema = yytext();
    return Tokens.Numero;
}

// NÚMERO SEGUIDO DE LETRAS
{D}+[a-zA-Z]+ {
    Lexema = yytext();
    return Tokens.ERROR;
}

// EMPIEZA CON GUION BAJO
"_"+[a-zA-Z0-9]* {
    Lexema = yytext();
    return Tokens.ERROR;
}

// PALABRA CON CARACTERES NO ASCII
{L}({L}|{D})*[^\x00-\x7F]+[a-zA-Z0-9]* {
    Lexema = yytext();
    return Tokens.ERROR;
}

// PALABRA QUE EMPIEZA CON CARACTERES NO ASCII
[^\x00-\x7F]+[a-zA-Z0-9]* {
    Lexema = yytext();
    return Tokens.ERROR;
}

// IDENTIFICADORES VÁLIDOS
{L}({L}|{D})* {
    Lexema = yytext();
    return Tokens.Identificador;
}

// ERROR GENERAL
. {
    Lexema = yytext();
    return Tokens.ERROR;
}