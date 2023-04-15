package edu.ewubd.contractlist;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private KeyValueDB kvDB;
    private SharedPreferences sp;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    ArrayList<ArrayList<String>> contactList = new ArrayList<>();
    ArrayList<String> labels = new ArrayList<>(Arrays.asList("Image", "Key", "Name", "Email", "Phone Home", "Phone Office"));
    private EditText etName, etEmail, etPhoneHome, etPhoneOffice;
    private ImageView ivPhoto;
    private Button btPhoto, btCancel, btSave;
    private String name, imageString = "", email, phoneHome, phoneOffice, values = "", errorMessages = "";

    private String checkString(String string) {
        if (string == null || string.isEmpty()) {
            string = "";
        }
        return string;
    }

    private String generateUniqueID() {
        return UUID.randomUUID().toString();
    }

    private void clearData() {
        etName.setText(null);
        etEmail.setText(null);
        etPhoneHome.setText(null);
        etPhoneOffice.setText(null);

        values = "";
        imageString = "";
        errorMessages = "";
        ivPhoto.setImageBitmap(null);
        btPhoto.setText("Photo");
    }

    private boolean validateData() {
        values = "";
        errorMessages = "";

        name = checkString(etName.getText().toString());
        email = checkString(etEmail.getText().toString());
        phoneHome = checkString(etPhoneHome.getText().toString());
        phoneOffice = checkString(etPhoneOffice.getText().toString());

        if (name.equals("")) {
            errorMessages += "Invalid name\n";
        }
        if (imageString.equals("")) {
            errorMessages += "Invalid photo\n";
        }
        if (email.equals("") || (!email.contains("@") && !email.contains("."))) {
            errorMessages += "Invalid email\n";
        }
        if (phoneHome.equals("") || phoneHome.length() != 11) {
            errorMessages += "Invalid phone home\n";
        }
        if (phoneOffice.equals("") || phoneOffice.length() != 11) {
            errorMessages += "Invalid phone office\n";
        }

        if(errorMessages.equals("")) {
            values += name + ", ";
            values += email + ", ";
            values += phoneHome + ", ";
            values += phoneOffice;
            return true;
        }

        return false;
    }

    private void getContactDataFromSP() {
        int c = 0;
        System.out.println("Contact information:\n");

        if (sp.contains("imageString")) {
            System.out.println("Image: " + sp.getString("imageString", ""));
            c++;
        }
        if (sp.contains("id")) {
            System.out.println("Key: " + sp.getString("id", ""));
            c++;
        }
        if (sp.contains("name")) {
            System.out.println("Name: " + sp.getString("name", ""));
            c++;
        }
        if (sp.contains("email")) {
            System.out.println("Email: " + sp.getString("email", ""));
            c++;
        }
        if (sp.contains("phoneHome")) {
            System.out.println("Phone Home: " + sp.getString("phoneHome", ""));
            c++;
        }
        if (sp.contains("phoneOffice")) {
            System.out.println("Phone Office: " + sp.getString("phoneOffice", ""));
            c++;
        }
        if (c == 0) {
            System.out.println("There is no saved contact information in Shared Preferences.");
        }
    }

    private void printContactDataFromSQLite() {
        System.out.println("Contact information from SQLite:\n");

        if(!contactList.isEmpty()) {
            for (int i = 0; i < contactList.size(); i++) {
                System.out.println("Contact " + i + ":");
                for (int j = 0; j < contactList.get(i).size(); j++) {
                    if(j == 0) {
                        System.out.println(labels.get(j) + ": " + contactList.get(i).get(j).hashCode());
                    } else {
                        System.out.println(labels.get(j) + ": " + contactList.get(i).get(j));
                    }
                }
            }
        } else {
            System.out.println("There is no saved contact information in SQLite.");
        }
    }

    private void selectPhoto() {
        ivPhoto.setImageBitmap(null);
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        activityResultLauncher.launch(intent);
    }

    private void showDialog(String message, String title, String btn01, String btn02, String key) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);

        dialog.setMessage(message);
        dialog.setTitle(title);

        dialog.setCancelable(false).setPositiveButton(btn01, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (btn01.equals("Yes")) {
                    try {
                        if (kvDB.insertKeyValue(key, values, imageString)) {
                            SharedPreferences.Editor editor = sp.edit();

                            editor.putString("id", key);
                            editor.putString("name", name);
                            editor.putString("imageString", imageString);
                            editor.putString("email", email);
                            editor.putString("phoneHome", phoneHome);
                            editor.putString("phoneOffice", phoneOffice);
                            editor.apply();

                            Toast.makeText(getApplicationContext(), "Contact saved successfully!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Error to insert into table", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    clearData();
                }
                dialog.cancel();
            }
        }).setNegativeButton(btn02, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialog.show();
    }

    private void dumpCursorToString(Cursor cursor) {
        int count = 0;
        System.out.println("\nDumping cursor to string " + cursor + " :\n");
        if (cursor != null) {
            int startPos = cursor.getPosition();
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                dumpCurrentRow(cursor);
                count++;
            }
            cursor.moveToPosition(startPos);
            if (count == 0) {
                System.out.println("No data in the cursor\n");
            } else {
                printContactDataFromSQLite();
            }
        } else {
            System.out.println("Cursor is null\n");
        }
    }

    private void dumpCurrentRow(Cursor cursor) {
        String[] cols = cursor.getColumnNames();
        int length = cols.length;
        contactList.add(new ArrayList<>());

        for (int i = 0; i < length; i++) {
            try {
                if (i == 0) {
                    contactList.get(contactList.size() - 1).add(cursor.getString(i));
                } else if (i == 1) {
                    String values = cursor.getString(i);
                    ArrayList<String> list = new ArrayList<String>(Arrays.asList(values.split(", ")));
                    for(int j = 0; j < list.size(); j++) {
                        contactList.get(contactList.size() - 1).add(list.get(j));
                    }
                } else {
                    String image = cursor.getString(i);
                    contactList.get(contactList.size() - 1).add(0, image);
                }
            } catch (SQLiteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            kvDB = new KeyValueDB(MainActivity.this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sp = getSharedPreferences("contactData", MODE_PRIVATE);
        setContentView(R.layout.activity_main);

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            if (result.getResultCode() == RESULT_OK && data != null) {
                Uri uri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] bytes = stream.toByteArray();
                    imageString = Base64.encodeToString(bytes, Base64.DEFAULT);
                    System.out.println(imageString);
                    ivPhoto.setImageBitmap(bitmap);
                    btPhoto.setText(null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                imageString = "";
                ivPhoto.setImageBitmap(null);
                btPhoto.setText("Photo");
            }
        });

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (Boolean.TRUE.equals(isGranted)) {
                selectPhoto();
            } else {
                Toast.makeText(getApplicationContext(), "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        });

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhoneHome = findViewById(R.id.etPhoneHome);
        etPhoneOffice = findViewById(R.id.etPhoneOffice);
        ivPhoto = findViewById(R.id.ivPhoto);
        btPhoto = findViewById(R.id.btPhoto);
        btCancel = findViewById(R.id.btCancel);
        btSave = findViewById(R.id.btSave);

        btCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                //finishAndRemoveTask();
                //System.exit(0);
            }
        });

        btPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                            android.Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
                } else {
                    //selectPhoto();
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
            }
        });

        btSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getContactDataFromSP();

                try {
                    Cursor cursor = kvDB.getAllKeyValues();
                    dumpCursorToString(cursor);
                    contactList.clear();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (validateData() && errorMessages.equals("")) {
                    showDialog("Do you want to save this contact information?", "Contact Information", "Yes", "Back", generateUniqueID());
                } else {
                    showDialog(errorMessages, "Error!", null, "Back", null);
                }
            }
        });
    }

    /*
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selectPhoto();
        } else {
            Toast.makeText(getApplicationContext(), "Permission Denied!", Toast.LENGTH_SHORT).show();
        }
    }
    */
}