/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pethoalpar.androidtesstwoocr;

/**
 *
 * @author felipe
 */
public class DatosExtra {
    public int columna;
    public int fila;
    public String lexema;
    public DatosExtra(String l, int f, int c){
        lexema = l;
        fila = f;
        columna = c;
    }
}
