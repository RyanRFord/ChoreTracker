package cpsc430.choretracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisplayCreateAccount extends AppCompatActivity {
    private List<String> list = new ArrayList<>();
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private String user;
    private String email;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_create_account);

        //Search for already logged in user
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        user = preferences.getString("Username", "");
        email = preferences.getString("Email", "").replace(".", ",");
        role = preferences.getString("Role", "");

        //Catch if user already logged in and if so send to view
        if(!user.equals("") && !email.equals("") && !role.equals("") && role.equals("Parent")){
            Intent intent = new Intent(this, parentView.class);
            startActivity(intent);
        }else if(!user.equals("") && !email.equals("") && !role.equals("") && role.equals("Child")){
            Intent intent = new Intent(this, childView.class);;
            startActivity(intent);
        }

        //Fill role spinner
        list.add("Choose Role:");
        list.add("Parent");
        list.add("Child");
        addToList();

        Button create = findViewById(R.id.buttonCreateAccount);
        create.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                DatabaseReference myRef = database.getReference();

                EditText email = findViewById(R.id.textEmail);
                String userEmail = EncodeString(email.getText().toString());

                myRef = myRef.child("Users").child(userEmail);

                myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    //Collect all data from user
                    EditText email = findViewById(R.id.textEmail);
                    String userEmail = EncodeString(email.getText().toString());
                    EditText username = findViewById(R.id.textReward);
                    String user = username.getText().toString();
                    EditText pass = findViewById(R.id.textPassword);
                    String password = pass.getText().toString();
                    Spinner role = findViewById(R.id.spinnerChooseRole);
                    String chosenRole = role.getSelectedItem().toString();
                    TextView error = findViewById(R.id.createError);

                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(email.getText().toString().equals("")){
                            error.setText("Enter an Email!");
                        }else if(username.getText().toString().equals("")) {
                            error.setText("Enter a Username!");
                        }else if(pass.getText().toString().equals("")) {
                            error.setText("Enter a Password!");
                        }else if(role.getSelectedItem().toString().equals("Choose Role:")){
                            error.setText("Select a Role!");
                        }else if (dataSnapshot.hasChild(user)) {
                            //Catch if an account with the username already exists
                            error.setText("Account Already Exists!");
                        }else if(!email.getText().toString().contains("@") || !email.getText().toString().contains(".")) {
                            //Check if email contains an '@' symbol and a '.' symbol
                            error.setText("Invalid Email!");
                        }else if(username.equals("Rewards") || username.equals("Chores")){
                            //Don't allow create account to overwrite chores/rewards lists
                            error.setText("Invalid Username!");
                        } else if(username.getText().toString().length() > 9) {
                            error.setText("Username cannot be more than 9 characters.");
                        }else {
                                Map<String, String> userData = new HashMap<>();
                                userData.put("Username", user);
                                userData.put("Email", userEmail);
                                userData.put("Password", password);
                                userData.put("Role", chosenRole);
                                if (chosenRole.equals("Child")) {
                                    userData.put("Stars", "0");
                                }
                                //Add data to database
                                dataSnapshot.getRef().child(user).setValue(userData);


                                if(chosenRole.equals("Parent")){
                                    //Go-To Parent View
                                    Intent intent = new Intent(v.getContext(), parentView.class);
                                    intent.putExtra("user", user);
                                    addLocal(user, userEmail, chosenRole);
                                    startActivity(intent);
                                }else if(chosenRole.equals("Child")){
                                    //Go-To Child View
                                    Intent intent = new Intent(v.getContext(), childView.class);
                                    intent.putExtra("user", user);
                                    addLocal(user, userEmail, chosenRole);
                                    startActivity(intent);
                                }
                            }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        });
    }

    @Override
    //If user tries to go back after creating an account it will direct them to home page
    protected void onStart() {
        super.onStart();
        //Search for already logged in user
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        user = preferences.getString("Username", "");
        email = preferences.getString("Email", "").replace(".", ",");
        role = preferences.getString("Role", "");

        //User logged in currently is a parent
        if(!user.equals("") && !email.equals("") && !role.equals("") && role.equals("Parent")){
            Intent intent = new Intent(this, main.class);
            startActivity(intent);
        //User logged in currently a child
        }else if(!user.equals("") && !email.equals("") && !role.equals("") && role.equals("Child")){
            Intent intent = new Intent(this, main.class);
            startActivity(intent);
        }
    }

    //Adds a session for the user who logged in
    public void addLocal(String user, String email, String role){
        //Add user to signed in
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("Username", user);
        editor.putString("Email", email);
        editor.putString("Role", role);
        editor.apply();
    }

    //Replaces . with , to allow the string to be put into database
    public static String EncodeString(String string) {
        return string.replace(".", ",");
    }

    //Replaces , with . to correct the string back to original
    public static String DecodeString(String string) {
        return string.replace(",", ".");
    }

    //Updates visual of dropdown
    public void addToList(){
        Spinner dropdown = findViewById(R.id.spinnerChooseRole);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropdown.setAdapter(dataAdapter);
    }
}
