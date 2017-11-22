package cn.com.heaton.blelibrary.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;


/**This class is provide interface for BLE states
 * Created by liulei on 2016/11/25.
 */

public abstract class BleLisenter {
    /**
     *  The notification describes when the write succeeded
     * @param gatt gatt
     */
    public void onDescriptorWriter(BluetoothGatt gatt){};

    /**
     *  Reads when the notification description is successful
     * @param gatt gatt
     */
    public void onDescriptorRead(BluetoothGatt gatt){};

}
