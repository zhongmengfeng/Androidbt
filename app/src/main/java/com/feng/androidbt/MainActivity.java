package com.feng.androidbt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv_statue)
    TextView tvStatue;
    @BindView(R.id.open)
    Button open;
    @BindView(R.id.close)
    Button close;
    @BindView(R.id.search)
    Button search;
    @BindView(R.id.search_device)
    Button searchDevice;
    @BindView(R.id.recycle)
    RecyclerView recycle;
    private BluetoothAdapter bluetoothAdapter;
    private RecycleAdapter adapter;
    //定义一个列表，存蓝牙设备的地址。
    public ArrayList<String> arrayList = new ArrayList<>();
    //定义一个列表，存蓝牙设备地址，用于显示。
    public ArrayList<String> deviceName = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);//注册广播接收信号
        registerReceiver(bluetoothReceiver, intentFilter);//用BroadcastReceiver 来取得结果
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) return;
        adapter = new RecycleAdapter();
        recycle.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        recycle.setAdapter(adapter);
    }


    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {//找到设备 添加数据
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                deviceName.add("设备名："+device.getName()+"\n" +"设备地址："+device.getAddress() + "\n");//将搜索到的蓝牙名称和地址添加到列表。
                arrayList.add( device.getAddress());//将搜索到的蓝牙地址添加到列表。
                Log.e("reviver","获取"+device.getName());
                adapter.notifyDataSetChanged();//更新
                adapter.setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onClick(int position) {
                        final BluetoothSocket socket;
                        try {
                            socket = (BluetoothSocket) device.getClass().getDeclaredMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1);
                            bluetoothAdapter.cancelDiscovery();//adapter为获取到的蓝牙适配器
                            try {
                                socket.connect();//连接
                            } catch (IOException e) {
                                Toast.makeText(MainActivity.this,"连接失败",0).show();
                                e.printStackTrace();
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        }
    };

    @OnClick({R.id.open, R.id.close,R.id.search_device,R.id.search})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.open:
                if (!bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.enable();//打开蓝牙
                }
                break;
            case R.id.close:
                if (bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.disable();//关闭蓝牙
                }
                break;
            case R.id.search://设置设备可被搜索到
                if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) //不在可被搜索的范围
                {
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);//设置本机蓝牙在300秒内可见
                    startActivity(discoverableIntent);
                }
                break;
            case R.id.search_device:
                doDiscovry();
                break;
        }
    }

    public void doDiscovry() {
        if (bluetoothAdapter.isDiscovering()) {
            Log.e("doDiscovry","quxiao");
            //判断蓝牙是否正在扫描，如果是调用取消扫描方法；如果不是，则开始扫描
            bluetoothAdapter.cancelDiscovery();
        } else {
            Log.e("doDiscovry","开始");
            bluetoothAdapter.startDiscovery();
        }

    }

    class RecycleAdapter extends RecyclerView.Adapter<RecycleAdapter.MyHolder> {
        //第一步 定义接口

        private OnItemClickListener listener;

        //第二步， 写一个公共的方法
        public void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }
        @NonNull
        @Override
        public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.recycle_item, parent, false);
            MyHolder myHolder = new MyHolder(view);
            return myHolder;
        }

        @Override
        public void onBindViewHolder(MyHolder holder, int position) {
            holder.textView.setText(deviceName.get(position) );
            holder.textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(listener!=null){
                        listener.onClick(position);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return arrayList.size();
        }

        public class MyHolder extends RecyclerView.ViewHolder {
            TextView textView;

            public MyHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.item_tv);
            }
        }

    }
    public interface OnItemClickListener {
        void onClick(int position);
    }

    protected void onDestroy(){
        super.onDestroy();//解除注册
        unregisterReceiver(bluetoothReceiver);
    }



}