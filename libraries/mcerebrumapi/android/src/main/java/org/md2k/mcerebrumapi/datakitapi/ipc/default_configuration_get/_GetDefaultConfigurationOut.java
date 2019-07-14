package org.md2k.mcerebrumapi.datakitapi.ipc.default_configuration_get;
/*
 * Copyright (c) 2016, The University of Memphis, MD2K Center

 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import android.os.Bundle;

import com.alibaba.fastjson.JSON;

import org.md2k.mcerebrumapi.datakitapi.ipc.OperationType;
import org.md2k.mcerebrumapi.datakitapi.ipc._Session;
import org.md2k.mcerebrumapi.status.MCStatus;

import java.util.HashMap;

public class _GetDefaultConfigurationOut {
    public static _Session create(int session, HashMap<String, Object> configuration) {
        Bundle b = new Bundle();
        String str = JSON.toJSONString(configuration);
        b.putString(HashMap.class.getSimpleName(), str);
        return new _Session(session, OperationType.GET_DEFAULT_CONFIGURATION, MCStatus.SUCCESS, b);
    }

    public static HashMap<String, Object> getConfiguration(Bundle b) {
        if (b == null) return null;
        String str = b.getString(HashMap.class.getSimpleName());
        return JSON.parseObject(str, HashMap.class);
    }
}
