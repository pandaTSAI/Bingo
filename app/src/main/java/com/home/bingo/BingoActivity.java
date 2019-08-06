package com.home.bingo;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BingoActivity extends AppCompatActivity implements ValueEventListener {

    private static final int NUMBER_COUNT = 25;
    private static final String TAG = BingoActivity.class.getSimpleName();
    private String roomKey;
    private boolean creator;
    private TextView info;
    private RecyclerView recyclerView;
    private List<NumberBall> numbers;
    private List<Button> buttons;
    Map<Integer, Integer> numberPositions = new HashMap<>();
    private NumberAdapter adapter;
    private ValueEventListener statusListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            if (dataSnapshot.getValue() == null)
                return;
            long status = (long) dataSnapshot.getValue();
            switch((int) status) {
                case Room.STATUS_INIT:
                    info.setText("等待對手進入遊戲室");
                    break;
                case Room.STATUS_JOINED:
                    info.setText("YA! 對手加入了");
                    if (isCreator()){
                        myTurn = true;
                        FirebaseDatabase.getInstance().getReference("rooms")
                                .child(roomKey)
                                .child("status")
                                .setValue(Room.STATUS_CREATORS_TURN);
                    }
                    break;
                case Room.STATUS_CREATORS_TURN:
                    info.setText(isCreator() ? "請選擇號碼" : "等待對手選號");
                    break;
                case Room.STATUS_JOINERS_TURN:
                    info.setText(!isCreator() ? "請選擇號碼" : "等待對手選號");
                    setMyTurn(true);
                    break;
                case Room.STATUS_CREATOR_BINGO:
                    String msg = isCreator() ? "你賓果了!讚" : "對方賓果了";
                    new AlertDialog.Builder(BingoActivity.this)
                            .setTitle("賓果")
                            .setMessage(msg)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finishGame();
                                }
                            }).show();
                    break;
                case Room.STATUS_JOINER_BINGO:
                    String msg2 = !isCreator() ? "你賓果了!讚" : "對方賓果了";
                    new AlertDialog.Builder(BingoActivity.this)
                            .setTitle("賓果")
                            .setMessage(msg2)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finishGame();
                                }
                            }).show();
                    break;
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    private void finishGame() {
        if (isCreator()){
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomKey)
                    .removeValue();
        }
        finish();
    }

    public boolean isMyTurn() {
        return myTurn;
    }

    public void setMyTurn(boolean myTurn) {
        this.myTurn = myTurn;
    }

    private boolean myTurn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bingo);
        roomKey = getIntent().getStringExtra("ROOM_KEY");
        creator = getIntent().getBooleanExtra("IS_CREATOR",false);
        generateRandoms();
        if (isCreator()){
            for (int i = 0; i < NUMBER_COUNT; i++) {
                FirebaseDatabase.getInstance().getReference("rooms")
                        .child(roomKey)
                        .child("numbers")
                        .child((i+1) + "")
                        .setValue(false);
            }
        } else { //joiner
            FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomKey)
                    .child("status")
                    .setValue(Room.STATUS_JOINED);
        }
        findView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomKey)
                .child("numbers")
                .addValueEventListener(this);
        FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomKey)
                .child("status")
                .addValueEventListener(statusListener);

    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomKey)
                .child("status").removeEventListener(statusListener);
    }

    private void generateRandoms() {
        // generate random number
        numbers = new ArrayList<>();
        buttons = new ArrayList<>();
        for (int i = 1; i <= NUMBER_COUNT; i++) {
            numbers.add(new NumberBall(i));
        }
        Collections.shuffle(numbers);
        for (int i = 0; i < NUMBER_COUNT; i++) {
            Button button = new Button(this);
            button.setText(numbers + "");
            buttons.add(button);
            numberPositions.put(numbers.get(i).getNumber(), i);
        }
    }

    private void findView() {
        info = findViewById(R.id.info);
        recyclerView = findViewById(R.id.recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this,5));
        adapter = new NumberAdapter();
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        int[] nums = new int[NUMBER_COUNT];
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            boolean picked = (boolean) snapshot.getValue();
            int num = Integer.parseInt(snapshot.getKey());
            nums[numberPositions.get(num)] = picked ? 1 : 0 ;
            if (picked) {
                numbers.get(numberPositions.get(num)).setPicked(true);
            }
            adapter.notifyDataSetChanged();
            //bingo process
//            Log.d(TAG, "onDataChange: " + nums);
            int bingo = 0;
            for (int i = 0; i < 5; i++) {
                int sum = 0;
                for (int j = 0; j < 5; j++){
                    sum += nums[i * 5 + j];
                }
                if (sum == 5)
                    bingo++;
                sum = 0;
                for (int j = 0; j < 5; j++) {
                    sum += nums[i + j * 5];
                }
                if (sum == 5)
                    bingo++;
            }
            Log.d(TAG, "onDataChange: Bingo: " + bingo);
            if (bingo > 0 ){
                FirebaseDatabase.getInstance().getReference("rooms")
                        .child(roomKey)
                        .child("status")
                        .setValue(isCreator() ? Room.STATUS_CREATOR_BINGO : Room.STATUS_JOINER_BINGO);
            }
        }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {

    }

    class NumberAdapter extends RecyclerView.Adapter<NumberAdapter.NumberHolder> {
        @NonNull
        @Override
        public NumberHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new NumberHolder(getLayoutInflater().inflate(R.layout.number_item,viewGroup,false));
        }

        @Override
        public void onBindViewHolder(@NonNull final NumberHolder holder, final int position) {
            holder.button.setText(numbers.get(position).getNumber() + "");
            holder.button.setTag(position);
            holder.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (myTurn) {
                        Log.d(TAG, "onClick: number:" + numbers.get(position));
                        FirebaseDatabase.getInstance().getReference("rooms")
                                .child(roomKey)
                                .child("numbers")
                                .child(numbers.get(position).getNumber() + "")
                                .setValue(true);
                        holder.button.setEnabled(false);
                        FirebaseDatabase.getInstance().getReference("rooms")
                                .child(roomKey)
                                .child("status")
                                .setValue(isCreator()? Room.STATUS_JOINERS_TURN : Room.STATUS_CREATORS_TURN);
                        setMyTurn(false);
                    }
                }
            });
            holder.button.setEnabled(!numbers.get(position).isPicked());
        }

        @Override
        public int getItemCount() {
            return NUMBER_COUNT;
        }

        class NumberHolder extends RecyclerView.ViewHolder {
            Button button;
            public NumberHolder(@NonNull View itemView) {
                super(itemView);
                button = itemView.findViewById(R.id.button);
            }
        }
    }

    public boolean isCreator() {
        return creator;
    }

    public void setCreator(boolean creator) {
        this.creator = creator;
    }
}
