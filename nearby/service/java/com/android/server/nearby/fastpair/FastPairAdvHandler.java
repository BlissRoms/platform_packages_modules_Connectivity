/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.nearby.fastpair;

import android.content.Context;
import android.nearby.FastPairDevice;
import android.nearby.NearbyDevice;
import android.util.Log;

import com.android.server.nearby.common.locator.Locator;
import com.android.server.nearby.fastpair.halfsheet.FastPairHalfSheetManager;
import com.android.server.nearby.provider.FastPairDataProvider;
import com.android.server.nearby.util.FastPairDecoder;
import com.android.server.nearby.util.Hex;

import com.google.protobuf.ByteString;

import service.proto.Cache;
import service.proto.Rpcs;

/**
 * Handler that handle fast pair related broadcast.
 */
public class FastPairAdvHandler {
    Context mContext;
    String mBleAddress;

    /** The types about how the bloomfilter is processed. */
    public enum ProcessBloomFilterType {
        IGNORE, // The bloomfilter is not handled. e.g. distance is too far away.
        CACHE, // The bloomfilter is recognized in the local cache.
        FOOTPRINT, // Need to check the bloomfilter from the footprints.
        ACCOUNT_KEY_HIT // The specified account key was hit the bloom filter.
    }

    /**
     * Constructor function.
     */
    public FastPairAdvHandler(Context context) {
        mContext = context;
    }

    /**
     * Handles all of the scanner result. Fast Pair will handle model id broadcast bloomfilter
     * broadcast and battery level broadcast.
     */
    public void handleBroadcast(NearbyDevice device) {
        FastPairDevice fastPairDevice = (FastPairDevice) device;
        if (mBleAddress != null && mBleAddress.equals(fastPairDevice.getBluetoothAddress())) {
            return;
        }
        mBleAddress = fastPairDevice.getBluetoothAddress();
        if (FastPairDecoder.checkModelId(fastPairDevice.getData())) {
            byte[] model = FastPairDecoder.getModelId(fastPairDevice.getData());
            Log.d("FastPairService",
                    "On discovery model id" + Hex.bytesToStringLowercase(model));
            // Use api to get anti spoofing key from model id.
            Rpcs.GetObservedDeviceResponse response =
                    FastPairDataProvider.getInstance()
                            .loadFastPairAntispoofkeyDeviceMetadata(model);
            ByteString publicKey = response.getDevice().getAntiSpoofingKeyPair().getPublicKey();
            Locator.get(mContext, FastPairHalfSheetManager.class).showHalfSheet(
                    Cache.ScanFastPairStoreItem.newBuilder().setAddress(mBleAddress)
                            .setAntiSpoofingPublicKey(publicKey)
                            .build());
        } else {
            // Start to process bloomfilter

        }
    }
}
