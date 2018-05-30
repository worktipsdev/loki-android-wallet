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

package com.m2049r.xmrwallet.util;

public class UserNotes {
    public String txNotes = "";
    public String note = "";

    public UserNotes(final String txNotes) {
        if (txNotes == null) {
            return;
        }
        this.txNotes = txNotes;
        note = txNotes;
    }

    public void setNote(String newNote) {
        if (newNote != null) {
            note = newNote;
        } else {
            note = "";
        }
        txNotes = buildTxNote();
    }

    private String buildTxNote() {
        return note;
    }
}