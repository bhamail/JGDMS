/* Copyright (c) 2010-2012 Zeus Project Services Pty Ltd.
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

package org.apache.river.concurrent;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Executor;
import java.lang.ref.Reference;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author peter
 */
public class ReferenceBlockingQueueTest {
    private BlockingQueue<String> instance;
    public ReferenceBlockingQueueTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
        instance = RC.blockingQueue(new ArrayBlockingQueue<Referrer<String>>(30), Ref.SOFT, 10000L);
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of put method, of class ReferenceBlockingQueue.
     */
    @Test
    public void testPut() throws Exception {
        System.out.println("put");
        String e = "put";
        instance.put(e);
        String r = instance.take();
        assertEquals(e, r);
    }

    /**
     * Test of offer method, of class ReferenceBlockingQueue.
     */
    @Test
    public void testOffer() throws Exception {
        System.out.println("offer");
        String e = "offer";
        long timeout = 2L;
        TimeUnit unit = TimeUnit.MILLISECONDS;
        boolean expResult = true;
        boolean result = instance.offer(e, timeout, unit);
        assertEquals(expResult, result);
        result = instance.remove(e);
        assertEquals(expResult, result);
    }

    /**
     * Test of take method, of class ReferenceBlockingQueue.
     */
    @Test
    public void testTake() throws Exception {
        System.out.println("take");
        String expResult = "take";
        instance.add(expResult);
        Object result = instance.take();
        assertEquals(expResult, result);
    }

    /**
     * Test of poll method, of class ReferenceBlockingQueue.
     */
    @Test
    public void testPoll() throws Exception {
        System.out.println("poll");
        long timeout = 10L;
        TimeUnit unit = TimeUnit.MILLISECONDS;
        String expResult = "poll";
        instance.add(expResult);
        Object result = instance.poll(timeout, unit);
        assertEquals(expResult, result);
    }

    /**
     * Test of remainingCapacity method, of class ReferenceBlockingQueue.
     */
    @Test
    public void testRemainingCapacity() {
        System.out.println("remainingCapacity");
        int expResult = 30;
        int result = instance.remainingCapacity();
        assertEquals(expResult, result);
    }

    /**
     * Test of drainTo method, of class ReferenceBlockingQueue.
     */
    @Test
    public void testDrainTo_Collection() {
        System.out.println("drainTo");
        Collection<String> c = new ArrayList<String>();
        instance.add("drain");
        instance.add("two");
        int expResult = 2;
        int result = instance.drainTo(c);
        assertEquals(expResult, result);
        assertTrue(c.contains("drain"));
        assertTrue(c.contains("two"));
    }

    /**
     * Test of drainTo method, of class ReferenceBlockingQueue.
     */
    @Test
    public void testDrainTo_Collection_int() {
        System.out.println("drainTo");
        Collection<String> c = new ArrayList<String>();
        instance.add("drain");
        instance.add("too");
        int maxElements = 1;
        int expResult = 1;
        int result = instance.drainTo(c, maxElements);
        assertEquals(expResult, result);
    }
    
      
    /**
     * Test serialization
     */
    @Test
    @SuppressWarnings("unchecked")
    public void serialization() {
        System.out.println("Serialization Test");
        Object result = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            out = new ObjectOutputStream(baos);
            out.writeObject(instance);
            // Unmarshall it
            in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
            result = in.readObject();
        } catch (IOException ex) {
            ex.printStackTrace(System.out);
        } catch (ClassNotFoundException ex){
            ex.printStackTrace(System.out);
        }
        assertTrue(result instanceof BlockingQueue);
        assertTrue(instance.containsAll((BlockingQueue<String>)result));
    }
}
