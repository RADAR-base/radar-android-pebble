package org.radarcns.empaticaE4;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import org.apache.avro.specific.SpecificRecord;
import org.radarcns.android.DeviceStatusListener;
import org.radarcns.data.AvroDecoder;
import org.radarcns.data.Record;
import org.radarcns.data.SpecificRecordDecoder;
import org.radarcns.kafka.AvroTopic;
import org.radarcns.kafka.rest.ServerStatusListener;
import org.radarcns.key.MeasurementKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.radarcns.empaticaE4.E4Service.DEVICE_STATUS_CHANGED;
import static org.radarcns.empaticaE4.E4Service.DEVICE_STATUS_NAME;
import static org.radarcns.empaticaE4.E4Service.TRANSACT_GET_DEVICE_STATUS;
import static org.radarcns.empaticaE4.E4Service.TRANSACT_GET_RECORDS;
import static org.radarcns.empaticaE4.E4Service.TRANSACT_GET_SERVER_STATUS;
import static org.radarcns.empaticaE4.E4Service.TRANSACT_START_RECORDING;

class E4ServiceConnection implements ServiceConnection {
    private final static Logger logger = LoggerFactory.getLogger(E4ServiceConnection.class);
    private final MainActivity mainActivity;
    private boolean isRemote;
    private DeviceStatusListener.Status deviceStatus;
    public String deviceName;
    private IBinder serviceBinder;

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DEVICE_STATUS_CHANGED)) {
                if (intent.hasExtra(DEVICE_STATUS_NAME)) {
                    deviceName = intent.getStringExtra(DEVICE_STATUS_NAME);
                }
                deviceStatus = DeviceStatusListener.Status.values()[intent.getIntExtra(DEVICE_STATUS_CHANGED, 0)];
                logger.info("Updated device status to {}", deviceStatus);
                mainActivity.deviceStatusUpdated(E4ServiceConnection.this, deviceStatus);
            }
        }
    };
    private String apiKey;
    private String groupId;
    private String schemaRegistryUrl;
    private String kafkaUrl;

    E4ServiceConnection(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.serviceBinder = null;
        this.deviceName = null;
        this.deviceStatus = DeviceStatusListener.Status.DISCONNECTED;
        this.isRemote = false;
    }

    @Override
    public void onServiceConnected(final ComponentName className,
                                   IBinder service) {
        mainActivity.registerReceiver(statusReceiver, new IntentFilter(DEVICE_STATUS_CHANGED));

        if (serviceBinder == null) {
            logger.info("Bound to service {}", className);
            serviceBinder = service;

            // We've bound to the running Service, cast the IBinder and get instance
            mainActivity.serviceConnected(this);
            if (!(serviceBinder instanceof E4Service.LocalBinder)) {
                isRemote = true;
                try {
                    this.serviceBinder.linkToDeath(new IBinder.DeathRecipient() {
                        @Override
                        public void binderDied() {
                            onServiceDisconnected(className);
                            mainActivity.deviceStatusUpdated(E4ServiceConnection.this, deviceStatus);
                            bind(kafkaUrl, schemaRegistryUrl, groupId, apiKey);
                        }
                    }, 0);
                } catch (RemoteException e) {
                    logger.error("Failed to link to death", e);
                }
            }
            try {
                deviceStatus = getDeviceData().getStatus();
            } catch (RemoteException e) {
                logger.error("Failed to get device status", e);
            }
        } else {
            logger.info("Trying to re-bind service, from {} to {}", serviceBinder, service);
        }
    }

    public <V extends SpecificRecord> List<Record<MeasurementKey, V>> getRecords(AvroTopic<MeasurementKey, V> topic, int limit) throws RemoteException, IOException {
        LinkedList<Record<MeasurementKey, V>> result = new LinkedList<>();

        if (isRemote) {
            Parcel data = Parcel.obtain();
            data.writeString(topic.getName());
            data.writeInt(limit);
            Parcel reply = Parcel.obtain();
            serviceBinder.transact(TRANSACT_GET_RECORDS, data, reply, 0);
            AvroDecoder.AvroReader<MeasurementKey> keyDecoder = new SpecificRecordDecoder(true).reader(topic.getKeySchema(), MeasurementKey.class);
            AvroDecoder.AvroReader<V> valueDecoder = new SpecificRecordDecoder(true).reader(topic.getValueSchema(), topic.getValueClass());

            int len = reply.readInt();
            for (int i = 0; i < len; i++) {
                long offset = reply.readLong();
                MeasurementKey key = keyDecoder.decode(reply.createByteArray());
                V value = valueDecoder.decode(reply.createByteArray());
                result.addFirst(new Record<>(offset, key, value));
            }
        } else {
            for (Record<MeasurementKey, V> record : ((E4Service.LocalBinder)serviceBinder).getRecords(topic, limit)) {
                result.addFirst(record);
            }
        }

        return result;
    }

    public void startRecording() throws RemoteException {
        if (isRemote) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            serviceBinder.transact(TRANSACT_START_RECORDING, data, reply, 0);
            deviceStatus = E4DeviceStatus.CREATOR.createFromParcel(reply).getStatus();
        } else {
            deviceStatus = ((E4Service.LocalBinder)serviceBinder).startRecording().getStatus();
        }
    }

    public void stopRecording() throws RemoteException {
        if (isRemote) {
            Parcel data = Parcel.obtain();
            serviceBinder.transact(TRANSACT_START_RECORDING, data, null, 0);
        } else {
            ((E4Service.LocalBinder)serviceBinder).stopRecording();
        }
    }

    public boolean isRecording() {
        return deviceStatus != DeviceStatusListener.Status.DISCONNECTED;
    }

    public boolean isScanning() {
        return deviceStatus == DeviceStatusListener.Status.READY;
    }

    public boolean hasService() {
        return serviceBinder != null;
    }

    public ServerStatusListener.Status getServerStatus() throws RemoteException {
        if (isRemote) {
            Parcel reply = Parcel.obtain();
            serviceBinder.transact(TRANSACT_GET_SERVER_STATUS, Parcel.obtain(), reply, 0);
            return ServerStatusListener.Status.values()[reply.readInt()];
        } else {
            return ((E4Service.LocalBinder)serviceBinder).getServerStatus();
        }
    }

    public E4DeviceStatus getDeviceData() throws RemoteException {
        if (isRemote) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            serviceBinder.transact(TRANSACT_GET_DEVICE_STATUS, data, reply, 0);

            return E4DeviceStatus.CREATOR.createFromParcel(reply);
        } else {
            return (E4DeviceStatus)((E4Service.LocalBinder)serviceBinder).getDeviceStatus();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        serviceBinder = null;
        deviceName = null;
        deviceStatus = DeviceStatusListener.Status.DISCONNECTED;
        mainActivity.serviceDisconnected(this);
        mainActivity.unregisterReceiver(statusReceiver);
    }

    void bind(String kafkaUrl, String schemaRegistryUrl, String groupId, String apiKey) {
        this.kafkaUrl = kafkaUrl;
        this.schemaRegistryUrl = schemaRegistryUrl;
        this.groupId = groupId;
        this.apiKey = apiKey;
        logger.info("Intending to start E4 service");

        Intent e4serviceIntent = new Intent(mainActivity, E4Service.class);
        e4serviceIntent.putExtra("kafka_rest_proxy_url", kafkaUrl);
        e4serviceIntent.putExtra("schema_registry_url", schemaRegistryUrl);
        e4serviceIntent.putExtra("group_id", groupId);
        e4serviceIntent.putExtra("empatica_api_key", apiKey);
        mainActivity.startService(e4serviceIntent);
        mainActivity.bindService(e4serviceIntent, this, Context.BIND_ABOVE_CLIENT);
    }

    public void unbind() {
        mainActivity.unbindService(this);
        onServiceDisconnected(null);
    }

    public String getDeviceName() {
        return deviceName;
    }

    public DeviceStatusListener.Status getDeviceStatus() {
        return deviceStatus;
    }
}
