package cn.com.heaton.blelibrary.ble.model;

/**
 * Created by jerry on 2019/2/13.
 */
import android.os.Build;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * description $desc$
 * created by jerry on 2019/02/21.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ScanRecord {

    /**
     * Returns a string composed from a {@link SparseArray}.
     */
    static String toString(SparseArray<byte[]> array) {
        if (array == null) {
            return "null";
        }
        if (array.size() == 0) {
            return "{}";
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append('{');
        for (int i = 0; i < array.size(); ++i) {
            buffer.append(array.keyAt(i)).append("=").append(Arrays.toString(array.valueAt(i)));
        }
        buffer.append('}');
        return buffer.toString();
    }


    /**
     * Returns a string composed from a {@link Map}.
     */
    static <T> String toString(Map<T, byte[]> map) {
        if (map == null) {
            return "null";
        }
        if (map.isEmpty()) {
            return "{}";
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append('{');
        Iterator<Map.Entry<T, byte[]>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<T, byte[]> entry = it.next();
            Object key = entry.getKey();
            buffer.append(key).append("=").append(Arrays.toString(map.get(key)));
            if (it.hasNext()) {
                buffer.append(", ");
            }
        }
        buffer.append('}');
        return buffer.toString();
    }

    private static final String TAG = "ScanRecord";

    // The following data type values are assigned by Bluetooth SIG.
    // For more details refer to Bluetooth 4.1 specification, Volume 3, Part C, Section 18.
    private static final int DATA_TYPE_FLAGS = 0x01;
    private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL = 0x02;
    private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE = 0x03;
    private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL = 0x04;
    private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE = 0x05;
    private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL = 0x06;
    private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE = 0x07;
    private static final int DATA_TYPE_LOCAL_NAME_SHORT = 0x08;
    private static final int DATA_TYPE_LOCAL_NAME_COMPLETE = 0x09;
    private static final int DATA_TYPE_TX_POWER_LEVEL = 0x0A;
    private static final int DATA_TYPE_SERVICE_DATA = 0x16;
    private static final int DATA_TYPE_MANUFACTURER_SPECIFIC_DATA = 0xFF;

    // Flags of the advertising data.
    private final int mAdvertiseFlags;

    @Nullable
    private final List<ParcelUuid> mServiceUuids;

    private final SparseArray<byte[]> mManufacturerSpecificData;

    private final Map<ParcelUuid, byte[]> mServiceData;

    // Transmission power level(in dB).
    private final int mTxPowerLevel;

    // Local name of the Bluetooth LE device.
    private final String mDeviceName;

    // Raw bytes of scan record.
    private final byte[] mBytes;

    /**
     * Returns the advertising flags indicating the discoverable mode and capability of the device.
     * Returns -1 if the flag field is not set.
     */
    public int getAdvertiseFlags() {
        return mAdvertiseFlags;
    }

    /**
     * Returns a list of service UUIDs within the advertisement that are used to identify the
     * bluetooth GATT services.
     */
    public List<ParcelUuid> getServiceUuids() {
        return mServiceUuids;
    }

    /**
     * Returns a sparse array of manufacturer identifier and its corresponding manufacturer specific
     * data.
     */
    public SparseArray<byte[]> getManufacturerSpecificData() {
        return mManufacturerSpecificData;
    }

    /**
     * Returns the manufacturer specific data associated with the manufacturer id. Returns
     * {@code null} if the {@code manufacturerId} is not found.
     */
    @Nullable
    public byte[] getManufacturerSpecificData(int manufacturerId) {
        return mManufacturerSpecificData.get(manufacturerId);
    }

    /**
     * Returns a map of service UUID and its corresponding service data.
     */
    public Map<ParcelUuid, byte[]> getServiceData() {
        return mServiceData;
    }

    /**
     * Returns the service data byte array associated with the {@code serviceUuid}. Returns
     * {@code null} if the {@code serviceDataUuid} is not found.
     */
    @Nullable
    public byte[] getServiceData(ParcelUuid serviceDataUuid) {
        if (serviceDataUuid == null) {
            return null;
        }
        return mServiceData.get(serviceDataUuid);
    }

    /**
     * Returns the transmission power level of the packet in dBm. Returns {@link Integer#MIN_VALUE}
     * if the field is not set. This value can be used to calculate the path loss of a received
     * packet using the following equation:
     * <p>
     * <code>pathloss = txPowerLevel - rssi</code>
     */
    public int getTxPowerLevel() {
        return mTxPowerLevel;
    }

    /**
     * Returns the local name of the BLE device. The is a UTF-8 encoded string.
     */
    @Nullable
    public String getDeviceName() {
        return mDeviceName;
    }

    /**
     * Returns raw bytes of scan record.
     */
    public byte[] getBytes() {
        return mBytes;
    }

    private ScanRecord(List<ParcelUuid> serviceUuids,
                       SparseArray<byte[]> manufacturerData,
                       Map<ParcelUuid, byte[]> serviceData,
                       int advertiseFlags, int txPowerLevel,
                       String localName, byte[] bytes) {
        mServiceUuids = serviceUuids;
        mManufacturerSpecificData = manufacturerData;
        mServiceData = serviceData;
        mDeviceName = localName;
        mAdvertiseFlags = advertiseFlags;
        mTxPowerLevel = txPowerLevel;
        mBytes = bytes;
    }

    /**
     * Parse scan record bytes to {@link android.bluetooth.le.ScanRecord}.
     * <p>
     * The format is defined in Bluetooth 4.1 specification, Volume 3, Part C, Section 11 and 18.
     * <p>
     * All numerical multi-byte entities and values shall use little-endian <strong>byte</strong>
     * order.
     *
     * @param scanRecord The scan record of Bluetooth LE advertisement and/or scan response.
     * @hide
     */
    public static ScanRecord parseFromBytes(byte[] scanRecord) {
        if (scanRecord == null) {
            return null;
        }
        int currentPos = 0;
        int advertiseFlag = -1;
        List<ParcelUuid> serviceUuids = new ArrayList<ParcelUuid>();
        String localName = null;
        int txPowerLevel = Integer.MIN_VALUE;

        SparseArray<byte[]> manufacturerData = new SparseArray<byte[]>();
        Map<ParcelUuid, byte[]> serviceData = new ArrayMap<ParcelUuid, byte[]>();

        try {
            while (currentPos < scanRecord.length) {
                // length is unsigned int.
                int length = scanRecord[currentPos++] & 0xFF;
                if (length == 0) {
                    break;
                }
                // Note the length includes the length of the field type itself.
                int dataLength = length - 1;
                // fieldType is unsigned int.
                int fieldType = scanRecord[currentPos++] & 0xFF;
                switch (fieldType) {
                    case DATA_TYPE_FLAGS:
                        advertiseFlag = scanRecord[currentPos] & 0xFF;
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos,
                                dataLength,16, serviceUuids);
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos, dataLength,
                                32, serviceUuids);
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos, dataLength,
                                128, serviceUuids);
                        break;
                    case DATA_TYPE_LOCAL_NAME_SHORT:
                    case DATA_TYPE_LOCAL_NAME_COMPLETE:
                        localName = new String(
                                extractBytes(scanRecord, currentPos, dataLength));
                        break;
                    case DATA_TYPE_TX_POWER_LEVEL:
                        txPowerLevel = scanRecord[currentPos];
                        break;
                    case DATA_TYPE_SERVICE_DATA:
                        // The first two bytes of the service data are service data UUID in little
                        // endian. The rest bytes are service data.
                        int serviceUuidLength = 16;
                        byte[] serviceDataUuidBytes = extractBytes(scanRecord, currentPos,
                                serviceUuidLength);
                        ParcelUuid serviceDataUuid = parseUuidFrom(
                                serviceDataUuidBytes);
                        byte[] serviceDataArray = extractBytes(scanRecord,
                                currentPos + serviceUuidLength, dataLength - serviceUuidLength);
                        serviceData.put(serviceDataUuid, serviceDataArray);
                        break;
                    case DATA_TYPE_MANUFACTURER_SPECIFIC_DATA:
                        // The first two bytes of the manufacturer specific data are
                        // manufacturer ids in little endian.
                        int manufacturerId = ((scanRecord[currentPos + 1] & 0xFF) << 8) +
                                (scanRecord[currentPos] & 0xFF);
                        byte[] manufacturerDataBytes = extractBytes(scanRecord, currentPos + 2,
                                dataLength - 2);
                        manufacturerData.put(manufacturerId, manufacturerDataBytes);
                        break;
                    default:
                        // Just ignore, we don't handle such data type.
                        break;
                }
                currentPos += dataLength;
            }

            if (serviceUuids.isEmpty()) {
                serviceUuids = null;
            }
            return new ScanRecord(serviceUuids, manufacturerData, serviceData,
                    advertiseFlag, txPowerLevel, localName, scanRecord);
        } catch (Exception e) {
            Log.e(TAG, "unable to parse scan record: " + Arrays.toString(scanRecord));
            // As the record is invalid, ignore all the parsed results for this packet
            // and return an empty record with raw scanRecord bytes in results
            return new ScanRecord(null, manufacturerData, null, -1, Integer.MIN_VALUE, null, scanRecord);
        }
    }

    @Override
    public String toString() {
        return "ScanRecord [mAdvertiseFlags=" + mAdvertiseFlags + ", mServiceUuids=" + mServiceUuids
                + ", mManufacturerSpecificData=" + ScanRecord.toString(mManufacturerSpecificData)
                + ", mServiceData=" + ScanRecord.toString(mServiceData)
                + ", mTxPowerLevel=" + mTxPowerLevel + ", mDeviceName=" + mDeviceName + "]";
    }

    /**
     * byte数组转化为string
     */
    static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    // Parse service UUIDs.
    private static int parseServiceUuid(byte[] scanRecord, int currentPos, int dataLength,
                                        int uuidLength, List<ParcelUuid> serviceUuids) {
        while (dataLength > 0) {
            byte[] uuidBytes = extractBytes(scanRecord, currentPos,
                    uuidLength);
            serviceUuids.add(parseUuidFrom(uuidBytes));
            dataLength -= uuidLength;
            currentPos += uuidLength;

        }
        return currentPos;
    }

    // Helper method to extract bytes from byte array.
    private static byte[] extractBytes(byte[] scanRecord, int start, int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(scanRecord, start, bytes, 0, length);
        return bytes;
    }

    /**
     * 转化方法
     * @param uuidBytes
     * @return
     */
    public static ParcelUuid parseUuidFrom(byte[] uuidBytes) {
        if (uuidBytes == null) {
            throw new IllegalArgumentException("uuidBytes cannot be null");
        }
        int length = uuidBytes.length;
        if (length != 16 && length != 32 &&
                length != 128) {
            throw new IllegalArgumentException("uuidBytes length invalid - " + length);
        }

        // Construct a 128 bit UUID.
        if (length == 128) {
            ByteBuffer buf = ByteBuffer.wrap(uuidBytes).order(ByteOrder.LITTLE_ENDIAN);
            long msb = buf.getLong(8);
            long lsb = buf.getLong(0);
            return new ParcelUuid(new UUID(msb, lsb));
        }

        // For 16 bit and 32 bit UUID we need to convert them to 128 bit value.
        // 128_bit_value = uuid * 2^96 + BASE_UUID
        long shortUuid;
        if (length == 16) {
            shortUuid = uuidBytes[0] & 0xFF;
            shortUuid += (uuidBytes[1] & 0xFF) << 8;
        } else {
            shortUuid = uuidBytes[0] & 0xFF ;
            shortUuid += (uuidBytes[1] & 0xFF) << 8;
            shortUuid += (uuidBytes[2] & 0xFF) << 16;
            shortUuid += (uuidBytes[3] & 0xFF) << 24;
        }
        long msb = BASE_UUID.getUuid().getMostSignificantBits() + (shortUuid << 32);
        long lsb = BASE_UUID.getUuid().getLeastSignificantBits();
        return new ParcelUuid(new UUID(msb, lsb));
    }

    public static final ParcelUuid BASE_UUID =
            ParcelUuid.fromString("00000000-0000-1000-8000-00805F9B34FB");


}

