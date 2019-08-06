package com.home.bingo;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.Group;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener, ValueEventListener, View.OnClickListener {

    public static final int RC_SIGN_IN = 100;
    private static final String TAG = MainActivity.class.getSimpleName();
    private FirebaseAuth auth;
    private FirebaseUser user;
    private TextView nickText;
    private Member member;
    private ImageView avatar;
    private Group avatarGroup;
    private RecyclerView recyclerView;
    int[] avatars = {
            R.drawable.avatar_0,
            R.drawable.avatar_1,
            R.drawable.avatar_2,
            R.drawable.avatar_3,
            R.drawable.avatar_4,
            R.drawable.avatar_5,
            R.drawable.avatar_6};
    private FirebaseRecyclerAdapter<Room, RoomHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findView();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText titleEdit = new EditText(MainActivity.this);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Room title")
                        .setView(titleEdit)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String roomTitle = titleEdit.getText().toString();
                                DatabaseReference roomRef = FirebaseDatabase.getInstance().getReference("rooms").push();
                                Room room = new Room(roomTitle,member);
                                roomRef.setValue(room);
                                String key = roomRef.getKey();
                                Log.d(TAG, "onClick: Room key: "+key);
                                roomRef.child("key").setValue(key);
                                //TODO : enter game room
                                Intent bingo = new Intent(MainActivity.this,BingoActivity.class);
                                bingo.putExtra("ROOM_KEY", key);
                                bingo.putExtra("IS_CREATOR",true);
                                startActivity(bingo);
                            }
                        }).setNeutralButton("Cancel",null)
                        .show();
            }
        });
        auth = FirebaseAuth.getInstance();

    }

    private void findView() {
        nickText = findViewById(R.id.nickname);
        avatar = findViewById(R.id.avatar);
        avatarGroup = findViewById(R.id.group);
        recyclerView = findViewById(R.id.recycler);
        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                avatarGroup.setVisibility(avatarGroup.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });
        findViewById(R.id.avatar_0).setOnClickListener(this);
        findViewById(R.id.avatar_1).setOnClickListener(this);
        findViewById(R.id.avatar_2).setOnClickListener(this);
        findViewById(R.id.avatar_3).setOnClickListener(this);
        findViewById(R.id.avatar_4).setOnClickListener(this);
        findViewById(R.id.avatar_5).setOnClickListener(this);
        findViewById(R.id.avatar_6).setOnClickListener(this);
        //recyclerView
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        Query query = FirebaseDatabase.getInstance().getReference("rooms").orderByKey();
        FirebaseRecyclerOptions<Room> options = new FirebaseRecyclerOptions.Builder<Room>()
                .setQuery(query, Room.class).build();
        adapter = new FirebaseRecyclerAdapter<Room, RoomHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull RoomHolder holder, int position, @NonNull final Room model) {
                holder.title.setText(model.getTitle());
                holder.image.setImageResource(avatars[model.getCreator().getAvatar()]);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent bingo = new Intent(MainActivity.this,BingoActivity.class);
                        bingo.putExtra("ROOM_KEY",model.getKey());
                        bingo.putExtra("IS_CREATOR",false);
                        startActivity(bingo);
                    }
                });
            }

            @NonNull
            @Override
            public RoomHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
                View view = getLayoutInflater().inflate(R.layout.item_room, parent,false);
                return new RoomHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
    }

    class RoomHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;
        public RoomHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.room_image);
            title = itemView.findViewById(R.id.room_title);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    protected void onStart() {
        super.onStart();
        auth.addAuthStateListener(this);
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        auth.removeAuthStateListener(this);
        adapter.stopListening();
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
        switch (id){
            case R.id.action_settings:
                break;
            case R.id.action_signout:
                auth.signOut();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        user = firebaseAuth.getCurrentUser();
        if (user != null) {
            FirebaseDatabase.getInstance().getReference("users")
                    .child(user.getUid())
                    .child("uid")
                    .setValue(user.getUid());
            FirebaseDatabase.getInstance().getReference("users")
                    .child(user.getUid())
                    .addValueEventListener(this);
        } else {
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder()
            .setAvailableProviders(Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build(),
                    new AuthUI.IdpConfig.GoogleBuilder().build()
            )).setIsSmartLockEnabled(false).build(),
                    RC_SIGN_IN);
        }
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        member = dataSnapshot.getValue(Member.class);
        Log.d(TAG, "onDataChange: "+ member.getUid());
        if (member.getNickName() == null){
            final EditText nickEdit = new EditText(this);
            new AlertDialog.Builder(this)
                    .setTitle("Nickname")
                    .setView(nickEdit)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String nickname = nickEdit.getText().toString();
                            member.setNickName(nickname);
                            FirebaseDatabase.getInstance().getReference("users")
                                    .child(user.getUid())
                                    .setValue(member);
                        }
                    }).setNeutralButton("Cancel",null)
                    .show();
        }else {
            FirebaseDatabase.getInstance().getReference("users")
                    .child(user.getUid())
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            //member data change
                            member = dataSnapshot.getValue(Member.class);
                            nickText.setText(member.getNickName());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {

    }

    @Override
    public void onClick(View v) {
        int avatarId =0;
        switch (avatarId) {
            case R.id.avatar_0:
                avatarId = 0;
                break;
            case R.id.avatar_1:
                avatarId = 1;
                break;
            case R.id.avatar_2:
                avatarId = 2;
                break;
            case R.id.avatar_3:
                avatarId = 3;
                break;
            case R.id.avatar_4:
                avatarId = 4;
                break;
            case R.id.avatar_5:
                avatarId = 5;
                break;
            case R.id.avatar_6:
                avatarId = 6;
                break;
        }
        FirebaseDatabase.getInstance().getReference("users")
                .child(user.getUid())
                .child("avatar")
                .setValue(avatarId);
        avatarGroup.setVisibility(View.GONE);
    }
}
