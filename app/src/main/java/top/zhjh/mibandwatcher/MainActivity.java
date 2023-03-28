package top.zhjh.mibandwatcher;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import top.zhjh.mibandwatcher.databinding.ActivityMainBinding;
import top.zhjh.mibandwatcher.databinding.RowItemBinding;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = "MainActivity";

  /**
   * 视图绑定对象：简化视图元素的访问和类型转换
   */
  ActivityMainBinding mainBinding;
  BluetoothAdapter bluetoothAdapter;
  Context ctx = this;
  public List<BluetoothDevice> deviceList = new ArrayList<>();
  DeviceItemAdapter deviceItemAdapter;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // setContentView(R.layout.activity_main);
    // 使用视图绑定可以简化视图元素的访问和类型转换
    mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
    View view = mainBinding.getRoot();
    setContentView(view);

    // Android 12 及以上
    if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
      // 如果已经授予权限
      if ((ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)) {
        // 开始扫描
        startScan();
      } else {
        // 申请权限
        DexterCheck();
      }
    }
    // Android 12 以下
    else {
      // 申请权限
      DexterCheck();
    }

    // 设置 deviceRecycler 为垂直方向的线性布局
    mainBinding.deviceRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
    // 创建并设置 deviceRecycler 的适配器，管理子项的创建、绑定和布局等
    deviceItemAdapter = new DeviceItemAdapter();
    mainBinding.deviceRecycler.setAdapter(deviceItemAdapter);
  }

  // 创建扫描回调
  ScanCallback mScanCB = new ScanCallback() {
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
      super.onScanResult(callbackType, result);
      BluetoothDevice device = result.getDevice();
      String deviceName = device.getName();
      // 忽略没有名称的设备
      if (deviceName == null) {
        return;
      }
      Log.i(TAG, "扫描到 BLE 设备: " + deviceName + " - Address: " + device.getAddress());
      // 如果设备列表中不存在该设备
      if (!deviceList.stream().anyMatch(item -> item.getAddress().equals(device.getAddress()))) {
        // 则添加到设备列表中
        deviceList.add(device);
        // 通知 deviceRecycler 刷新设备列表
        deviceItemAdapter.notifyDataSetChanged();
      }
    }
  };

  /**
   * 开始扫描 BLE 设备
   */
  void startScan() {
    // 创建 BluetoothManager 对象，用于管理 BLE 通信
    BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    // 创建 BluetoothAdapter 对象，用于进行设备的扫描、连接和通信等操作
    bluetoothAdapter = bluetoothManager.getAdapter();
    // 开始扫描 BLE 设备
    bluetoothAdapter.getBluetoothLeScanner().startScan(mScanCB);
  }

  /**
   * 设备元素（子视图）适配器
   */
  public class DeviceItemAdapter extends RecyclerView.Adapter<DeviceItemAdapter.ChildViewHolder> {

    /**
     * 获取设备列表数量
     *
     * @return 设备列表数量
     */
    @Override
    public int getItemCount() {
      return deviceList.size();
    }

    /**
     * 创建和返回设备列表的子视图
     *
     * @param parent   RecyclerView 的父视图，即列表的容器视图
     * @param viewType 标识子视图的类型，可以在 onBindViewHolder() 中使用
     * @return
     */
    @NonNull
    @Override
    public DeviceItemAdapter.ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      // 使用 LayoutInflater 对象创建 device_item.xml 中定义的视图
      LayoutInflater inflater = LayoutInflater.from(parent.getContext());
      View itemView = inflater.inflate(R.layout.row_item, parent, false);
      // 与设备列表的子视图进行绑定
      return new ChildViewHolder(itemView);
    }

    /**
     * 绑定数据到设备列表的子视图上
     *
     * @param holder   ChildViewHolder 类型的对象，表示一个子视图的视图持有者，我们需要将数据绑定到该子视图上
     * @param position 子视图在列表中的位置
     */
    @Override
    public void onBindViewHolder(@NonNull DeviceItemAdapter.ChildViewHolder holder, int position) {
      BluetoothDevice device = deviceList.get(position);
      // 设置设备名称
      holder.row.rowItemName.setText(device.getName());
      // 设置蓝牙地址
      holder.row.rowItemAddress.setText(device.getAddress());
    }

    /**
     * 设备列表的子视图的视图持有者
     */
    class ChildViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

      // 该变量用于绑定子视图中的 UI 元素
      RowItemBinding row;

      /**
       * @param itemView 子视图中的根视图
       */
      ChildViewHolder(@NonNull View itemView) {
        super(itemView);
        // 将布局文件中的 UI 元素与 row 变量绑定
        row = RowItemBinding.bind(itemView);
        // 绑定用户点击事件
        row.cardViewItem.setOnClickListener(this);
      }

      @Override
      public void onClick(View itemView) {
        // 点击设备时停止扫描
        bluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCB);
        // 根据 getAdapterPosition() 获取点击的子视图在列表中的位置，根据位置获取设备列表中的设备（在扫描时添加进去的），并连接该设备
        connect(deviceList.get(getAdapterPosition()));

        // isStopScanning = true;
        Toast.makeText(ctx, "请查看日志", Toast.LENGTH_LONG).show();
        Log.i(TAG, "点击的设备：" + deviceList.get(getAdapterPosition()).getName() + " —— " + deviceList.get(getAdapterPosition()).getAddress());
      }
    }
  }

  public BluetoothGatt bluetoothGatt;

  /**
   * 连接设备
   *
   * @param device 要连接的设备
   */
  public void connect(BluetoothDevice device) {
    // 创建 BluetoothGatt 对象，用于连接 GATT 服务端
    bluetoothGatt = device.connectGatt(this, false, gattCallback);
  }

  /**
   * GATT 服务端的回调
   */
  private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
    /**
     * 连接状态改变时的回调
     * @param gatt 连接的 GATT 服务端对象
     * @param status 连接状态的状态码，参考 android.bluetooth.BluetoothGatt
     * @param newState 新的连接状态，参考 android.bluetooth.BluetoothProfile
     */
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
      Log.i(TAG, "连接状态改变 status: " + status + " newState:" + newState);
      // 连接成功
      if (status == BluetoothGatt.GATT_SUCCESS) {
        // 连接成功
        if (newState == BluetoothProfile.STATE_CONNECTED) {
          // 开始搜索服务
          bluetoothGatt.discoverServices();
        } else {
          // 连接失败时关闭 GATT 连接
          gatt.close();
          Log.i(TAG, "关闭 GATT 连接");
        }
      }
    }

    /**
     * 服务搜索完成时的回调
     * @param gatt 连接的 GATT 服务端对象
     * @param status 搜索状态的状态码，参考 android.bluetooth.BluetoothGatt
     */
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      // 搜索成功
      if (status == BluetoothGatt.GATT_SUCCESS) {
        // 获取 GATT 服务端提供的服务列表
        List<BluetoothGattService> gattServices = bluetoothGatt.getServices();
        BluetoothGattService foundService = null;
        for (BluetoothGattService gattServiceItem : gattServices) {
          // 获取服务的 UUID
          UUID uuid = gattServiceItem.getUuid();
          Log.i(TAG, "UUID: " + uuid.toString());
          foundService = gattServiceItem;

          // 电量
          if (uuid.toString().equals("0000180f-0000-1000-8000-00805f9b34fb")) {
            // 获取服务中的特征值列表
            List<BluetoothGattCharacteristic> gattCharacteristics = gattServiceItem.getCharacteristics();
            for (BluetoothGattCharacteristic gattCharacteristicItem : gattCharacteristics) {
              UUID uuid1 = gattCharacteristicItem.getUuid();
              // 电量值
              if (uuid1.toString().equals("00002a19-0000-1000-8000-00805f9b34fb")) {
                // 读取电量值
                bluetoothGatt.readCharacteristic(gattCharacteristicItem);
              }
            }
          }
        }
        // 没有找到服务
        if (foundService == null) {
          Log.i(TAG, "没有找到服务");
          return;
        } else {
          Log.i(TAG, "找到 " + gattServices.size() + " 个服务");
        }
      }
      // 搜索失败
      else {
        Log.i(TAG, "搜索失败");
      }
    }

    /**
     * 读取特征值时的回调
     * @param gatt 连接的 GATT 服务端对象
     * @param characteristic 读取的特征对象
     * @param status 读取状态的状态码，参考 android.bluetooth.BluetoothGatt
     */
    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
      super.onCharacteristicRead(gatt, characteristic, status);
      // 读取成功
      if (status == BluetoothGatt.GATT_SUCCESS) {
        Log.i(TAG, "读取特征值成功");
        // 获取特征对象的值
        byte[] value = characteristic.getValue();
        Log.i(TAG, "特征值：" + Arrays.toString(value));
      } else {
        Log.i(TAG, "读取特征值失败");
      }
    }

    /**
     * 写入特征值时的回调
     * @param gatt 连接的 GATT 服务端对象
     * @param characteristic 写入的特征对象
     * @param status 写入状态的状态码，参考 android.bluetooth.BluetoothGatt
     */
    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
      switch (status) {
        case BluetoothGatt.GATT_SUCCESS: {
          break;
        }
        case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED: {
          Log.i(TAG, "GATT_REQUEST_NOT_SUPPORTED");
          break;
        }
        case BluetoothGatt.GATT_READ_NOT_PERMITTED: {
          Log.i(TAG, "GATT_READ_NOT_PERMITTED");
          break;
        }
        case BluetoothGatt.GATT_WRITE_NOT_PERMITTED: {
          Log.i(TAG, "GATT_WRITE_NOT_PERMITTED");
          break;
        }
        default: {
          Log.i(TAG, "写入特征值失败");
          break;
        }
      }
    }

    /**
     * 读取描述符时的回调
     * @param gatt 连接的 GATT 服务端对象
     * @param descriptor 读取的描述符对象
     * @param status 读取状态的状态码，参考 android.bluetooth.BluetoothGatt
     */
    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
      super.onDescriptorWrite(gatt, descriptor, status);
      Log.i(TAG, "onDescriptorWrite");
      if (status != BluetoothGatt.GATT_SUCCESS) {
        return;
      }
    }

    /**
     * 接收通知或指示时的回调
     * @param gatt 连接的 GATT 服务端对象
     * @param characteristic 更新的特征对象
     */
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
      byte[] data = characteristic.getValue();
      Log.i(TAG, "onCharacteristicChanged");
    }
  };


  /**
   * Dexter 权限检查并申请
   */
  void DexterCheck() {
    String[] permissionArr;
    // 根据类型（Android 12 及以上）判断需要申请的权限
    if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
      // permissionArr = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN};
      permissionArr = new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN};
    } else {
      permissionArr = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    }

    // 权限申请
    Dexter.withContext(this).withPermissions(permissionArr).withListener(new MultiplePermissionsListener() {
      /**
       * 权限请求结果
       * @param report 权限请求结果
       */
      @Override
      public void onPermissionsChecked(MultiplePermissionsReport report) {
        // 所有权限已允许
        if (report.areAllPermissionsGranted()) {
          Log.i(TAG, "所有权限已允许");
          startScan();
        }
        // 有权限被永久拒绝
        if (report.isAnyPermissionPermanentlyDenied()) {
          List<PermissionDeniedResponse> a = report.getDeniedPermissionResponses();
          for (PermissionDeniedResponse item : a) {
            Log.i(TAG, "被永久拒绝的权限：" + item.getPermissionName());
            startScan();
          }
        }
      }

      /**
       * 选择了不再询问
       * @param list 权限列表
       * @param permissionToken 权限令牌
       */
      @Override
      public void onPermissionRationaleShouldBeShown(List<com.karumi.dexter.listener.PermissionRequest> list, PermissionToken permissionToken) {
        permissionToken.continuePermissionRequest();
      }
    }).check();
  }

}