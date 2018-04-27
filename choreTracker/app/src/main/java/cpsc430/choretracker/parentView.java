package cpsc430.choretracker;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class parentView extends AppCompatActivity {
    private List<String> starList = new ArrayList<>();
    private List<String> choreList = new ArrayList<>();
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private String user;
    private String email;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_view);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        user = preferences.getString("Username", "");
        email = preferences.getString("Email", "").replace(".", ",");
        role = preferences.getString("Role", "");

        TextView accountName = findViewById(R.id.accountName);
        accountName.setText(user);

        //Set title for chore list spinner
        choreList.add("Current Chores:");
        addToList(choreList, 2);

        //Set star value spinner
        starList.add("Chore Star Value:");
        for(int i = 1; i <= 10; i++){
            starList.add(i + "");
        }
        addToList(starList, 1);

        updateList();
    }

    //Fill spinners
    public void addToList(List L, int choice){
        Spinner dropdown;
        if(choice == 1){
            dropdown = findViewById(R.id.spinnerStarValue);
        }else{
            dropdown = findViewById(R.id.choreList);
        }
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, L);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropdown.setAdapter(dataAdapter);
    }

    // Add a chore to the database
    public void addChore(View v) {
        // Collect user data
        EditText input = findViewById(R.id.choreText);
        String choreName = input.getText().toString();

        DatabaseReference myRef = database.getReference().child("Users").child(email).child("Chores");

        Spinner starValueSpinner = findViewById(R.id.spinnerStarValue);
        String starValue = starValueSpinner.getSelectedItem().toString();

        TextView error = findViewById(R.id.addRewardError);

        // Check user input
        if(choreName.equals("")) {
            // Chore name was left blank
            error.setText("Please enter a chore name.");
        } else if (starValue.equals("Chore Star Value:")) {
            // Star value was left blank
            error.setText("Please select a star value.");
        } else {
            // All required input is given
            choreName = encodeQuery(choreName);

            // Add the chore to the database
            Map<String, String> userData = new HashMap<>();
            userData.put("choreName", choreName);
            userData.put("starValue", starValue);
            myRef.child(choreName).setValue(userData);

            // Update the UI chore list
            updateList();

            // Reset the user interface
            input.setText("");
            starValueSpinner.setSelection(0, true);
            error.setText("");
            main.notification(v, decodeQuery(choreName) + " has been added!");
        }
    }

    // Remove a chore from the database
    public void removeChore(final View v) {
        // Get the selected chore from UI
        final Spinner selectChoreSpinner = findViewById(R.id.choreList);
        String choreName = selectChoreSpinner.getSelectedItem().toString();
        final String original = choreName;

        final TextView error = findViewById(R.id.addRewardError);

        if(choreName.equals("Current Chores:")) {
            // No chore was selected
            error.setText("Please select a chore");
        } else {
            choreName = choreName.substring(0, choreName.indexOf('('));
            choreName = encodeQuery(choreName);
            DatabaseReference myRef = database.getReference();
            myRef.child("Users").child(email).child("Chores").child(choreName).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    dataSnapshot.getRef().removeValue();
                    choreList.remove(original);
                    updateList();
                    selectChoreSpinner.setSelection(0, true);
                    error.setText("");
                    main.notification(v, original + " has been removed!");
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    public void logout(View v){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        finish();
    }

    public void updateList(){
        DatabaseReference myRef = database.getReference().child("Users").child(email).child("Chores");

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Loop through chores in database and display current chores
                for (DataSnapshot dsp : dataSnapshot.getChildren()) {
                    String name = dsp.child("choreName").getValue().toString();
                    String value = dsp.child("starValue").getValue().toString();

                    name = decodeQuery(name);

                    // Making sure no duplicates
                    if(!choreList.contains(name + "(" + value + ")")) {
                        // Adding and Displaying list to user
                        choreList.add(name + "(" + value + ")");
                        addToList(choreList, 2);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    //Go-to rewards page
    public void rewards(View v){
        finish();
        Intent intent = new Intent(this, Reward.class);
        startActivity(intent);
    }

    private String encodeQuery(String s) {
        while(s.contains(".")) {
            s = s.replace(".", "DOT");
        }
        while(s.contains("$")) {
            s = s.replace("$", "DOLLAR");
        }
        while(s.contains("[")) {
            s = s.replace("[", "LBRACKET");
        }
        while(s.contains("]")) {
            s = s.replace("]", "RBRACKET");
        }
        while(s.contains("#")) {
            s = s.replace("#", "POUND");
        }

        return s;
    }

    private String decodeQuery(String s) {
        while(s.contains("DOT")) {
            s = s.replace("DOT", ".");
        }
        while(s.contains("DOLLAR")) {
            s = s.replace("DOLLAR", "$");
        }
        while(s.contains("LBRACKET")) {
            s = s.replace("LBRACKET", "[");
        }
        while(s.contains("RBRACKET")) {
            s = s.replace("RBRACKET", "]");
        }
        while(s.contains("POUND")) {
            s = s.replace("POUND", "#");
        }

        return s;
    }
}
