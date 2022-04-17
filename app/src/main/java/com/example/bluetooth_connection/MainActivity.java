package com.example.bluetooth_connection;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private TextView listen , listDevices ;
    private ListView listView;
    private TextView msg_box , status , not_found;
    private EditText writeMsg ;
    private String string ,st_message , st_status;
    private MediaPlayer player;
    private ImageView send;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice[] btArray;

    private SendReceive sendReceive;

    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING=2;
    static final int STATE_CONNECTED=3;
    static final int STATE_CONNECTION_FAILED=4;
    static final int STATE_MESSAGE_RECEIVED=5;

    private int REQUEST_ENABLE_BLUETOOTH=1;

    private static final String APP_NAME = "bluetooth_connection";
    private static final UUID MY_UUID=UUID.fromString("8ce255c0-223a-11e0-ac64-0803450c9a66");

    private final static String default_notification_channel_id = "default" ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initId();

        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();

        if(!bluetoothAdapter.isEnabled())
        {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BLUETOOTH);

        }

        implementListeners();


    }

    private void listDevice() {

        Set<BluetoothDevice> bt=bluetoothAdapter.getBondedDevices();
        String[] strings=new String[bt.size()];
        btArray=new BluetoothDevice[bt.size()];
        int index=0;

        if( bt.size()>0)
        {
            not_found.setVisibility(View.GONE);

            for(BluetoothDevice device : bt)
            {
                btArray[index]= device;
                strings[index]=device.getName();
                index++;
            }
            ArrayAdapter<String> arrayAdapter=new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,strings);
            listView.setAdapter(arrayAdapter);
        }
    }

    private class SendReceive extends Thread
    {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive (BluetoothSocket socket)
        {
            bluetoothSocket=socket;
            InputStream tempIn=null;
            OutputStream tempOut=null;

            try {
                tempIn=bluetoothSocket.getInputStream();
                tempOut=bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream=tempIn;
            outputStream=tempOut;
        }

        public void run()
        {
            byte[] buffer=new byte[1024];
            int bytes;

            while (true)
            {
                try {
                    bytes=inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes)
        {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void implementListeners() {

//        listDevices.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Set<BluetoothDevice> bt=bluetoothAdapter.getBondedDevices();
//                String[] strings=new String[bt.size()];
//                btArray=new BluetoothDevice[bt.size()];
//                int index=0;
//
//                if( bt.size()>0)
//                {
//                    for(BluetoothDevice device : bt)
//                    {
//                        btArray[index]= device;
//                        strings[index]=device.getName();
//                        index++;
//                    }
//                    ArrayAdapter<String> arrayAdapter=new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,strings);
//                    listView.setAdapter(arrayAdapter);
//                }
//            }
//        });

//        listen.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                ServerClass serverClass=new ServerClass();
//                serverClass.start();
//            }
//        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ClientClass clientClass=new ClientClass(btArray[i]);
                clientClass.start();

                ServerClass serverClass=new ServerClass();
                serverClass.start();

                status.setText("درحال اتصال...");
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                st_message = writeMsg.getText().toString();
                st_status = status.getText().toString();

                if(TextUtils.isEmpty(st_message)){
                    Toast.makeText(MainActivity.this, "پیغام خود را بنویسید", Toast.LENGTH_SHORT).show();
                }else {
                    if (st_status == "متصل شد"){
                        string= String.valueOf(writeMsg.getText());
                        sendReceive.write(string.getBytes());
                        writeMsg.setText("");
                        Toast.makeText(MainActivity.this, "ارسال شد", Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(MainActivity.this, "به دستگاهی متصل نیست", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what)
            {
                case STATE_LISTENING:
                    status.setText("درحال شناسایی...");
                    break;
                case STATE_CONNECTING:
                    status.setText("درحال اتصال...");
                    break;
                case STATE_CONNECTED:
                    status.setText("متصل شد");
//                    status.setTextColor(getResources().getColor(R.color.connected));

                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("ارتباط ناموفق");
//                    status.setTextColor(getResources().getColor(R.color.connection_failed));
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuff= (byte[]) msg.obj;
                    String tempMsg=new String(readBuff,0,msg.arg1);
                    msg_box.setText(tempMsg);
                    msg_box.setTextColor(getResources().getColor(R.color.matn));
                    player= MediaPlayer.create(MainActivity.this,R.raw.notification);
                    player.start();
//                    Uri alarmSound = RingtoneManager. getDefaultUri (RingtoneManager. TYPE_NOTIFICATION);
//                    NotificationCompat.Builder mBuilder =
//                            new NotificationCompat.Builder(MainActivity.this,
//                                    default_notification_channel_id )
//                                    .setSmallIcon(R.drawable. ic_launcher_foreground )
//                                    .setVibrate( new long []{ 1000 , 1000 , 1000 , 1000 , 1000 })
//                                    .setContentTitle( "Test" )
//                                    .setSound(alarmSound)
//                                    .setContentText( "Hello! This is my first push notification" ) ;
//                    NotificationManager mNotificationManager = (NotificationManager)
//                            getSystemService(Context.NOTIFICATION_SERVICE);
//                    mNotificationManager.notify(( int ) System. currentTimeMillis (), mBuilder.build());
                    break;
            }
            return true;
        }
    });

    private class ServerClass extends Thread
    {
        private BluetoothServerSocket serverSocket;

        public ServerClass(){
            try {
                serverSocket=bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run()
        {
            BluetoothSocket socket=null;

            while (socket==null)
            {
                try {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTING;
                    handler.sendMessage(message);

                    socket=serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }

                if(socket!=null)
                {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTED;
                    handler.sendMessage(message);

                    sendReceive=new SendReceive(socket);
                    sendReceive.start();
                    break;
                }
            }
        }
    }

    private class ClientClass extends Thread
    {
        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass (BluetoothDevice device1)
        {
            device=device1;

            try {
                socket=device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run()
        {
            try {
                socket.connect();
                Message message=Message.obtain();
                message.what=STATE_CONNECTED;
                handler.sendMessage(message);

                sendReceive=new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
                Message message=Message.obtain();
                message.what=STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }




    private void initId() {
//        listen = findViewById(R.id.bt_listen);
        send = findViewById(R.id.bt_send);
//        listDevices = findViewById(R.id.bt_listdevice);
        listView = findViewById(R.id.listview_device);
        msg_box = findViewById(R.id.txt_message);
        status = findViewById(R.id.txt_status);
        writeMsg = findViewById(R.id.editxt_message);
        not_found = findViewById(R.id.txt_not_found);
    }

    @Override
    protected void onResume() {
        super.onResume();
        listDevice();
    }
}