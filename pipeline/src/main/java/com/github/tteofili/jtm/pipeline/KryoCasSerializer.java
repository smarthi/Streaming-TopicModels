/*
 * Copyright 2018 Tommaso Teofili and Simone Tripodi
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.tteofili.jtm.pipeline;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CASMgrSerializer;
import org.apache.uima.cas.impl.CASSerializer;
import org.apache.uima.cas.impl.FSIndexRepositoryImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;

public class KryoCasSerializer extends Serializer<CAS> {

  private JavaSerializer javaSerializer
      = new JavaSerializer();

  @Override
  public void write(Kryo kryo, Output output, CAS cas) {

    CASMgrSerializer mgrSerializer = new CASMgrSerializer();
    mgrSerializer.addTypeSystem((TypeSystemImpl) cas.getTypeSystem());
    mgrSerializer.addIndexRepository((FSIndexRepositoryImpl) cas.getIndexRepository());

    CASSerializer casSerializer = new CASSerializer();
    casSerializer.addCAS((CASImpl) cas);

    CASCompleteSerializer casCompleteSerializer = new CASCompleteSerializer();
    casCompleteSerializer.setCasMgrSerializer(mgrSerializer);
    casCompleteSerializer.setCasSerializer(casSerializer);

    javaSerializer.write(kryo, output, casCompleteSerializer);
  }

  @Override
  public CAS read(Kryo kryo, Input input, Class<CAS> type) {

    CASCompleteSerializer casSerializer =
        (CASCompleteSerializer) javaSerializer.read(kryo, input, CASCompleteSerializer.class);

    CASImpl cas = new CASImpl();
    cas.reinit((CASCompleteSerializer) casSerializer);

    return cas.getView("_InitialView");
  }
}
