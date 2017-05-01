package co.macrosystem.subirimagenes;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;

import static android.R.attr.path;
import static android.R.attr.permission;


public class MainActivity extends AppCompatActivity {

    final int CAMERA_REQUEST = 1;

    private FloatingActionButton camara;
    private  ImageView imagen;
    private TextView nombreImagen;
    private Button upload;
    private Uri output;
    private String foto;
    private File file;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {



        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Sube tus fotos");
        setSupportActionBar(toolbar);
        verifyStoragePermissions(this);

        camara = (FloatingActionButton) findViewById(R.id.camara);
        imagen = (ImageView) findViewById(R.id.imagen);
        nombreImagen = (EditText) findViewById(R.id.nombreImagen);
        upload = (Button) findViewById(R.id.upload);


        camara.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!nombreImagen.getText().toString().trim().equalsIgnoreCase("")){
                    getCamara();
                }else{
                    Snackbar.make(view, "Debe nombrar el archivo primero.", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (file.exists()) new ServerUpdate().execute();
                //Snackbar.make(v, "Enviando Imagen Al Servidor ...", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Creamos un objeto de la clase ContentResolver que nos dará acceso al contenido almacenado en la app
        ContentResolver cr = this.getContentResolver();
        //Creamos un objeto de la clase Bitmap para manejar nuestra imagen que hemos obtenido de nuestra cámara.
        Bitmap bit = null;
        try {
            bit = android.provider.MediaStore.Images.Media.getBitmap(cr, output);
            //orientacion
            int rotate = 0;
            //Creamos un objeto ExifInterface que se encargará de evaluar en que orientación esta nuestra imagen
            ExifInterface exif = new ExifInterface(
                    file.getAbsolutePath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation){
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
            //Creamos un objeto de la clase Matrix que será la encargada de transformar nuestra imagen y rotarla en su posición.
            Matrix matrix = new Matrix();
            matrix.postRotate(rotate);
            //e indicamos a Bitmap la nueva configuración de la imagen:
            bit = Bitmap.createBitmap(bit, 0, 0, bit.getWidth(), bit.getHeight(), matrix, true);
        }catch (Exception e){
            e.printStackTrace();
        }
        nombreImagen.setEnabled(false);
        //Finalmente insertamos nuestra imagen procesada a nuestro ImageView
        imagen.setImageBitmap(bit);

    }

    //persmission method.
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have read or write permission
        int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        int CameraPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);

        if (writePermission != PackageManager.PERMISSION_GRANTED || readPermission != PackageManager.PERMISSION_GRANTED || CameraPermission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );

        }
    }

    private void getCamara() {

        //Primeramente almacenamos en la String foto la ruta donde se va a guardar nuestra imagen, en el dispositivo, con el nombre que hayamos elegido.
        foto = Environment.getExternalStorageDirectory() + "/"
            + nombreImagen.getText().toString().trim() + ".jpg";
        //Pasamos nuestra String foto a File
        file = new File(foto);
        //Creamos un Intent que accederá a la cámara de nuestro dispositivo.
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //Pasamos nuestro File a Uri.
        output = Uri.fromFile(file);
        //Le pasamos a nuestro Intent la orden de almacenar en el dispositivo la imagen, y el Uri con la ruta de la imagen.
        intent.putExtra(MediaStore.EXTRA_OUTPUT, output);
        //Nos abre una nueva Activity con nuestra cámara y almacena el resultado para procesarla en el método sobreescrito starActivityForResult(parametro1,parametro2).
        //El 1 que esta almacenado en CAMERA_REQUEST es para indicar que queremos acceder a la cámara.
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    private void uploadFoto(String imag){
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpPost httppost = new HttpPost("http://192.168.1.7:80/dashboard/cobranzasmoviles/servicios/upload.php");
        MultipartEntity mpEntity = new MultipartEntity();
        ContentBody foto = new FileBody(file, "image/jpeg");
        mpEntity.addPart("fotoUp", foto);
        httppost.setEntity(mpEntity);
        try {
            httpclient.execute(httppost);
            httpclient.getConnectionManager().shutdown();
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private boolean onInsert(){
        HttpClient httpclient;
        List<NameValuePair> nameValuePairs;
        HttpPost httppost;
        httpclient=new DefaultHttpClient();
        httppost= new HttpPost("http://192.168.1.7:80/dashboard/cobranzasmoviles/servicios/insertImagen.php"); // Url del Servidor
        //Añadimos nuestros datos
        nameValuePairs = new ArrayList<NameValuePair>(1);
        nameValuePairs.add(new BasicNameValuePair("imagen",nombreImagen.getText().toString().trim()+".jpg"));

        try {
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            httpclient.execute(httppost);
            return true;
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    private void serverUpdate(){
        if (file.exists())new ServerUpdate().execute();
    }

    class ServerUpdate extends AsyncTask<String,String,String> {

        ProgressDialog pDialog;
        @Override
        protected String doInBackground(String... arg0) {
            uploadFoto(foto);
            if(onInsert())
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        Toast.makeText(MainActivity.this, "Éxito al subir la imagen",
                                Toast.LENGTH_LONG).show();
                    }
                });
            else
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        Toast.makeText(MainActivity.this, "Sin éxito al subir la imagen",
                                Toast.LENGTH_LONG).show();
                    }
                });
            return null;
        }
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Actualizando Servidor, espere..." );
            pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pDialog.show();
        }
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            pDialog.dismiss();
        }

    }
}







