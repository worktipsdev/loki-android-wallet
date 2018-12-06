/*
 * Copyright (c) 2017 m2049r
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

package com.m2049r.xmrwallet.data;

import android.net.Uri;

import com.m2049r.xmrwallet.model.Wallet;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class BarcodeData {
    public static final String XMR_SCHEME = "loki:";
    public static final String XMR_PAYMENTID = "tx_payment_id";
    public static final String XMR_AMOUNT = "tx_amount";

    public String address = null;
    public String paymentId = null;
    public String amount = null;

    public BarcodeData(String address) {
        this.address = address;
    }

    public BarcodeData(String address, String amount) {
        this.address = address;
        this.amount = amount;
    }

    public BarcodeData(String address, String paymentId, String amount) {
        this.address = address;
        this.paymentId = paymentId;
        this.amount = amount;
    }

    public Uri getUri() {
        return Uri.parse(getUriString());
    }

    public String getUriString() {
        StringBuilder sb = new StringBuilder();
        sb.append(BarcodeData.XMR_SCHEME).append(address);
        boolean first = true;
        if ((paymentId != null) && !paymentId.isEmpty()) {
            sb.append("?");
            first = false;
            sb.append(BarcodeData.XMR_PAYMENTID).append('=').append(paymentId);
        }
        if (!amount.isEmpty()) {
            if (first) {
                sb.append("?");
            } else {
                sb.append("&");
            }
            sb.append(BarcodeData.XMR_AMOUNT).append('=').append(amount);
        }
        return sb.toString();
    }

    static public BarcodeData fromQrCode(String qrCode) {
        // check for monero uri
        BarcodeData bcData = parseMoneroUri(qrCode);
        // check for naked monero address / integrated address
        if (bcData == null) {
            bcData = parseMoneroNaked(qrCode);
        }
        return bcData;
    }

    /**
     * Parse and decode a monero scheme string. It is here because it needs to validate the data.
     *
     * @param uri String containing a monero URL
     * @return BarcodeData object or null if uri not valid
     */

    static public BarcodeData parseMoneroUri(String uri) {
        Timber.d("parseMoneroUri=%s", uri);

        if (uri == null) return null;

        if (!uri.startsWith(XMR_SCHEME)) return null;

        String noScheme = uri.substring(XMR_SCHEME.length());
        Uri monero = Uri.parse(noScheme);
        Map<String, String> parms = new HashMap<>();
        String query = monero.getQuery();
        if (query != null) {
            String[] args = query.split("&");
            for (String arg : args) {
                String[] namevalue = arg.split("=");
                if (namevalue.length == 0) {
                    continue;
                }
                parms.put(Uri.decode(namevalue[0]).toLowerCase(),
                        namevalue.length > 1 ? Uri.decode(namevalue[1]) : "");
            }
        }
        String address = monero.getPath();
        String paymentId = parms.get(XMR_PAYMENTID);
        String amount = parms.get(XMR_AMOUNT);
        if (amount != null) {
            try {
                Double.parseDouble(amount);
            } catch (NumberFormatException ex) {
                Timber.d(ex.getLocalizedMessage());
                return null; // we have an amount but its not a number!
            }
        }
        if ((paymentId != null) && !Wallet.isPaymentIdValid(paymentId)) {
            Timber.d("paymentId invalid");
            return null;
        }

        if (!Wallet.isAddressValid(address)) {
            Timber.d("address invalid");
            return null;
        }
        return new BarcodeData(address, paymentId, amount);
    }

    static public BarcodeData parseMoneroNaked(String address) {
        Timber.d("parseMoneroNaked=%s", address);

        if (address == null) return null;

        if (!Wallet.isAddressValid(address)) {
            Timber.d("address invalid");
            return null;
        }

        return new BarcodeData(address);
    }

}
