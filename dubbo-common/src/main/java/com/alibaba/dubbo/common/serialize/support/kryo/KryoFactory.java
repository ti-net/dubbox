/**
 * Copyright 1999-2014 dangdang.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.alibaba.dubbo.common.serialize.support.kryo;

import java.lang.reflect.InvocationHandler;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.alibaba.dubbo.common.serialize.support.SerializableClassRegistry;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;

import de.javakaffee.kryoserializers.ArraysAsListSerializer;
import de.javakaffee.kryoserializers.BitSetSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptyListSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptyMapSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptySetSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonListSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonMapSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonSetSerializer;
import de.javakaffee.kryoserializers.DateSerializer;
import de.javakaffee.kryoserializers.GregorianCalendarSerializer;
import de.javakaffee.kryoserializers.JdkProxySerializer;
import de.javakaffee.kryoserializers.RegexSerializer;
import de.javakaffee.kryoserializers.SynchronizedCollectionsSerializer;
import de.javakaffee.kryoserializers.URISerializer;
import de.javakaffee.kryoserializers.UUIDSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;

/**
 * @author lishen
 */
public abstract class KryoFactory {

	// private static final KryoFactory factory = new PrototypeKryoFactory();
	// private static final KryoFactory factory = new SingletonKryoFactory();
	private static final KryoFactory factory = new PooledKryoFactory();

	private final Set<Class> registrations = new LinkedHashSet<Class>();

	private boolean registrationRequired;

	private volatile boolean kryoCreated;

	protected KryoFactory() {
		// TODO configurable
		// Log.DEBUG();
	}

	public static KryoFactory getDefaultFactory() {
		return factory;
	}

	/**
	 * only supposed to be called at startup time
	 *
	 * later may consider adding support for custom serializer, custom id, etc
	 */
	public void registerClass(Class clazz) {

		if (kryoCreated) {
			throw new IllegalStateException("Can't register class after creating kryo instance");
		}
		registrations.add(clazz);
	}

	protected Kryo createKryo() {
		if (!kryoCreated) {
			kryoCreated = true;
		}

		Kryo kryo = new CompatibleKryo() {
			@Override
			public Serializer<?> getDefaultSerializer(final Class type) {
				if (Date.class.isAssignableFrom(type)) {
					return new DateSerializer(type);
				}
				return super.getDefaultSerializer(type);
			}
		};

		// TODO
		// kryo.setReferences(false);
		kryo.setRegistrationRequired(registrationRequired);

		kryo.register(Arrays.asList("").getClass(), new ArraysAsListSerializer());
		kryo.register(Collections.EMPTY_LIST.getClass(), new CollectionsEmptyListSerializer());
		kryo.register(Collections.EMPTY_MAP.getClass(), new CollectionsEmptyMapSerializer());
		kryo.register(Collections.EMPTY_SET.getClass(), new CollectionsEmptySetSerializer());
		kryo.register(Collections.singletonList("").getClass(), new CollectionsSingletonListSerializer());
		kryo.register(Collections.singleton("").getClass(), new CollectionsSingletonSetSerializer());
		kryo.register(Collections.singletonMap("", "").getClass(), new CollectionsSingletonMapSerializer());
		kryo.register(GregorianCalendar.class, new GregorianCalendarSerializer());
		kryo.register(InvocationHandler.class, new JdkProxySerializer());
		kryo.register(BigDecimal.class, new DefaultSerializers.BigDecimalSerializer());
		kryo.register(BigInteger.class, new DefaultSerializers.BigIntegerSerializer());
		kryo.register(Pattern.class, new RegexSerializer());
		kryo.register(BitSet.class, new BitSetSerializer());
		kryo.register(URI.class, new URISerializer());
		kryo.register(UUID.class, new UUIDSerializer());
		UnmodifiableCollectionsSerializer.registerSerializers(kryo);
		SynchronizedCollectionsSerializer.registerSerializers(kryo);

		// now just added some very common classes
		// TODO optimization
		kryo.register(HashMap.class);
		kryo.register(ArrayList.class);
		kryo.register(LinkedList.class);
		kryo.register(HashSet.class);
		kryo.register(TreeSet.class);
		kryo.register(Hashtable.class);
		kryo.register(Date.class);
		kryo.register(Timestamp.class);
		kryo.register(Calendar.class);
		kryo.register(ConcurrentHashMap.class);
		kryo.register(SimpleDateFormat.class);
		kryo.register(GregorianCalendar.class);
		kryo.register(Vector.class);
		kryo.register(BitSet.class);
		kryo.register(StringBuffer.class);
		kryo.register(StringBuilder.class);
		kryo.register(Object.class);
		kryo.register(Object[].class);
		kryo.register(String[].class);
		kryo.register(byte[].class);
		kryo.register(char[].class);
		kryo.register(int[].class);
		kryo.register(float[].class);
		kryo.register(double[].class);

		for (Class clazz : registrations) {
			kryo.register(clazz);
		}

		for (Class clazz : SerializableClassRegistry.getRegisteredClasses()) {
			kryo.register(clazz);
		}

		return kryo;
	}

	public void returnKryo(Kryo kryo) {
		// do nothing by default
	}

	public void setRegistrationRequired(boolean registrationRequired) {
		this.registrationRequired = registrationRequired;
	}

	public void close() {
		// do nothing by default
	}

	public abstract Kryo getKryo();
}
