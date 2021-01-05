/***************************/
/* Based on a template by Oren Ish-Shalom */
/***************************/

/*************/
/* USER CODE */
/*************/
import java_cup.runtime.*;



/******************************/
/* DOLAR DOLAR - DON'T TOUCH! */
/******************************/

%%

/************************************/
/* OPTIONS AND DECLARATIONS SECTION */
/************************************/

/*****************************************************/
/* Lexer is the name of the class JFlex will create. */
/* The code will be written to the file Lexer.java.  */
/*****************************************************/
%class Lexer

/********************************************************************/
/* The current line number can be accessed with the variable yyline */
/* and the current column number with the variable yycolumn.        */
/********************************************************************/
%line
%column

/******************************************************************/
/* CUP compatibility mode interfaces with a CUP generated parser. */
/******************************************************************/
%cup

/****************/
/* DECLARATIONS */
/****************/
/*****************************************************************************/
/* Code between %{ and %}, both of which must be at the beginning of a line, */
/* will be copied verbatim (letter to letter) into the Lexer class code.     */
/* Here you declare member variables and functions that are used inside the  */
/* scanner actions.                                                          */
/*****************************************************************************/
%{
	/*********************************************************************************/
	/* Create a new java_cup.runtime.Symbol with information about the current token */
	/*********************************************************************************/
	private Symbol symbol(int type)               {return new Symbol(type, yyline, yycolumn);}
	private Symbol symbol(int type, Object value) {return new Symbol(type, yyline, yycolumn, value);}

	/*******************************************/
	/* Enable line number extraction from main */
	/*******************************************/
	public int getLine()    { return yyline + 1; }
	public int getCharPos() { return yycolumn;   }
%}

/***********************/
/* MACRO DECALARATIONS */
/***********************/
LineTerminator	    = \r|\n|\r\n
WhiteSpace		    = {LineTerminator} | [\t\f ]+

Comment             = {TraditionalComment} | {EndOfLineComment}
TraditionalComment  = "/*" [^*] ~"*/" | "/*" "*"+ "/"
EndOfLineComment    = "//" {InputCharacter}* {LineTerminator}?

Integer			    = 0 | [1-9][0-9]*
Identifier		    = [:jletter:][:jletterdigit:]*
ArraySign           = \[\]

/******************************/
/* DOLAR DOLAR - DON'T TOUCH! */
/******************************/

%%

/************************************************************/
/* LEXER matches regular expressions to actions (Java code) */
/************************************************************/

/**************************************************************/
/* YYINITIAL is the state at which the lexer begins scanning. */
/* So these regular expressions will only be matched if the   */
/* scanner is in the start state YYINITIAL.                   */
/**************************************************************/

<YYINITIAL> {
"public"                { return symbol(sym.PUBLIC); }
"class"                 { return symbol(sym.CLASS); }
"static"                { return symbol(sym.STATIC); }
"void"                  { return symbol(sym.VOID); }
"String"                { return symbol(sym.STRING_TYPE); }
"int"                   { return symbol(sym.INT_TYPE); }
"String"{ArraySign}     { return symbol(sym.STRING_ARRAY_TYPE); }
"int"{ArraySign}        { return symbol(sym.INT_ARRAY_TYPE); }
","			            { return symbol(sym.COMMA); }
"+"                     { return symbol(sym.PLUS); }
"-"                     { return symbol(sym.MINUS); }
"*"                     { return symbol(sym.MULT); }
"/"                     { return symbol(sym.DIV); }
"("                     { return symbol(sym.LPAREN); }
")"                     { return symbol(sym.RPAREN); }
"{"                     { return symbol(sym.LCURLY); }
"}"                     { return symbol(sym.RCURLY); }
";"                     { return symbol(sym.SEMICOLON); }
{Identifier}		    { return symbol(sym.ID, new String(yytext())); }
{Integer}               { return symbol(sym.NUMBER, Integer.parseInt(yytext())); }
{WhiteSpace}            { /* do nothing */ }
{Comment}               { /* do nothing */ }
<<EOF>>		            { return symbol(sym.EOF); }
}

// Handle comments