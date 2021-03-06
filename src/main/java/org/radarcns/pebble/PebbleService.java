/*
 * Copyright 2017 The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.radarcns.pebble;

import org.radarcns.android.device.DeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service that manages a Pebble2DeviceManager and a TableDataHandler to send store the data of a
 * Pebble 2 and send it to a Kafka REST proxy.
 */
public class PebbleService extends DeviceService<PebbleDeviceStatus> {
    private static final Logger logger = LoggerFactory.getLogger(PebbleService.class);

    @Override
    public void onCreate() {
        logger.info("Creating Pebble2 service {}", this);
        super.onCreate();
    }

    @Override
    protected PebbleDeviceManager createDeviceManager() {
        return new PebbleDeviceManager(this);
    }

    @Override
    protected PebbleDeviceStatus getDefaultState() {
        return new PebbleDeviceStatus();
    }
}
