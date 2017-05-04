package com.pethoalpar.androidtesstwoocr;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Path;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final String TESS_DATA = "/tessdata";
    private TextView textView;
    private List<Identificador> tokenslist;

    //Declaración de elementos nuevos.
    private TextView txtResultadoAnalizador;
    private EditText txtEditable;
    private Button btnAnalizar;
    private String textoAAnalizar;
    private ToggleButton toggleUSarTextoEditar;

    private TessBaseAPI tessBaseAPI;
    private Uri outputFileDir;
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString()+"/Tess";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) this.findViewById(R.id.textView);

        //Enlazado de elementos nuevos
        txtResultadoAnalizador = (TextView) this.findViewById(R.id.txtResultadoAnalizador);
        txtEditable = (EditText) this.findViewById(R.id.txtEditable);
        btnAnalizar = (Button)this.findViewById(R.id.btnAnalizar);
        toggleUSarTextoEditar = (ToggleButton)this.findViewById(R.id.toggleUSarTextoEditado);
        textoAAnalizar = "";

        final Activity activity = this;
        this.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},120);
                }
                if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},121);
                }
                startCameraActivity();
            }
        });

        //Evento clic en el nuevo boton
        btnAnalizar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    if(toggleUSarTextoEditar.isChecked()) {
                        textoAAnalizar = txtEditable.getText().toString();
                    }else{
                        textoAAnalizar = textView.getText().toString();
                    }

                    probarLexerFile(textoAAnalizar);
                    Toast.makeText(getApplicationContext(),"Se le envió el texto: "+textoAAnalizar,Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    txtResultadoAnalizador.setText("HUBO UN ERROR: "+e.getMessage());
                }
            }
        });
    }

    private void startCameraActivity(){
        try{
            String imagePath = DATA_PATH+ "/imgs";
            File dir = new File(imagePath);
            if(!dir.exists()){
                dir.mkdir();
            }
            String imageFilePath = imagePath+"/ocr.jpg";
            outputFileDir = Uri.fromFile(new File(imageFilePath));
            final Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileDir);
            if(pictureIntent.resolveActivity(getPackageManager() ) != null){
                startActivityForResult(pictureIntent,100);
            }
        } catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 100 && resultCode == Activity.RESULT_OK){
            prepareTessData();
            startOCR(outputFileDir);
        }else{
            Toast.makeText(getApplicationContext(),"Image problem",Toast.LENGTH_SHORT).show();
        }
    }

    private void prepareTessData(){
        try{
            File dir = new File(DATA_PATH + TESS_DATA);
            if(!dir.exists()){
                dir.mkdir();
            }
            String fileList[] = getAssets().list("");
            for(String fileName : fileList){
                String pathToDataFile = DATA_PATH+TESS_DATA+"/"+fileName;
                if(!(new File(pathToDataFile)).exists()){
                    InputStream in = getAssets().open(fileName);
                    OutputStream out = new FileOutputStream(pathToDataFile);
                    byte [] buff = new byte[1024];
                    int len ;
                    while(( len = in.read(buff)) > 0){
                        out.write(buff,0,len);
                    }
                    in.close();
                    out.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void startOCR(Uri imageUri){
        try{
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 7;
            Bitmap bitmap = BitmapFactory.decodeFile(imageUri.getPath(),options);
            String result = this.getText(bitmap);
            textView.setText(result);
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
    }

    private String getText(Bitmap bitmap){
        try{
            tessBaseAPI = new TessBaseAPI();
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        tessBaseAPI.init(DATA_PATH,"eng");
        tessBaseAPI.setImage(bitmap);
        String retStr = "No result";
        try{
            retStr = tessBaseAPI.getUTF8Text();
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        tessBaseAPI.end();
        return retStr;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 120:{
                if(grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "Read permission denied", Toast.LENGTH_SHORT).show();
                }
            }
            case 121:{
                if(grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "Write permission denied", Toast.LENGTH_SHORT).show();
                }
            }
            return;
        }
    }



    //Metodo sacado del ejemplo
    private void probarLexerFile(String textoAAnalizar) throws IOException{
        tokenslist = new LinkedList<Identificador>();
        int contIDs=0;
        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File fichero = new File(path, "/" + "fichero.txt");

        PrintWriter writer;
        try {
            writer = new PrintWriter(fichero);
            //writer.print(textView.getText());//entiendo q aca se escribe en el fichero y textView es en campo
            writer.print(textoAAnalizar);//de texto que contiene lo que se detecto con la camara
            writer.close();
        } catch (FileNotFoundException ex) {
            Toast toast = Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT);
            toast.show();
        }
        Reader reader = new BufferedReader(new FileReader(fichero.getAbsolutePath()));
        Lexer lexer = new Lexer(reader);
        String resultado="";
        while (true){
            Token token =lexer.yylex();
            if (token == null){
                for(int i=0;i<tokenslist.size();i++){
                    //System.out.println(tokenslist.get(i).nombre + "=" + tokenslist.get(i).ID);
                }
                txtResultadoAnalizador.setText(resultado);
                return;
            }
            switch (token){
                case SUMA:
                    resultado=resultado+ "<+>";
                    break;
                case RESTA:
                    resultado=resultado+ "<->";
                    break;
                case MULT:
                    resultado=resultado+ "<*>";
                    break;
                case DIV:
                    resultado=resultado+ "</>";
                    break;
                case ASSIGN:
                    resultado=resultado+ "<=>";
                    break;
                case ERROR:
                    resultado=resultado+ "Error, simbolo no reconocido ";
                    break;
                case ID: {
                    contIDs++;
                    Identificador tokenitem=new Identificador();
                    tokenitem.nombre=lexer.lexeme;
                    tokenitem.ID=contIDs;
                    tokenslist.add(tokenitem);
                    resultado=resultado+ "<ID" + contIDs + "> ";
                    break;
                }
                case INT:
                    resultado=resultado+ "< " + lexer.lexeme + "> ";
                    break;
                default:
                    resultado=resultado+ "<"+ lexer.lexeme + "> ";
            }
        }
    }
}
