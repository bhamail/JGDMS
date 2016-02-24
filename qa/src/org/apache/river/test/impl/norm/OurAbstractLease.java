/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.river.test.impl.norm;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import net.jini.core.lease.*;
import org.apache.river.api.io.AtomicSerial;
import org.apache.river.api.io.AtomicSerial.GetArg;
import org.apache.river.api.io.AtomicSerial.ReadInput;
import org.apache.river.api.io.AtomicSerial.ReadObject;

/**
 * Lifted from org.apache.river.lease.AbstractLease so we can have a codebase
 * independent from the service under test.
 * <p>
 * A base class for implementing lease objects.  This class takes care of
 * absolute vs relative time issues and implements some of the Lease methods.
 * The subclass is responsible for implementing: doRenew, cancel,
 * createLeaseMap, canBatch, hashCode, equals, and serialization of
 * any subclass state.
 */
@AtomicSerial
public abstract class OurAbstractLease implements Lease, java.io.Serializable {

    private static final long serialVersionUID = -9067179156916102052L;

    /**
     * The lease expiration, in local absolute time.
     */
    protected transient long expiration;
    /**
     * Serialization format for the expiration.
     *
     * @serial
     */
    protected int serialFormat = Lease.DURATION;

    /** Construct a relative-format lease. */
    protected OurAbstractLease(long expiration) {
	this.expiration = expiration;
    }
    
    /**
     * AtomicSerial constructor
     * @param arg
     * @throws IOException 
     */
    protected OurAbstractLease(GetArg arg) throws IOException{
	this( expiration(((RO)arg.getReader()).expiration,
		arg.get("serialFormat", Lease.DURATION)),
		arg.get("serialFormat", Lease.DURATION)
	);
    }
    
    private static long expiration(long expiration, int serialFormat){
	if (serialFormat == Lease.DURATION) {
	    boolean canOverflow = (expiration > 0);
	    expiration += System.currentTimeMillis();

	    // If we added two positive numbers, and the result is negative
	    // we must have overflowed, truncate to Long.MAX_VALUE. If we
	    // added a negative to a positive and the result is negative we
	    // must have underflowed, so limit to zero.
	    if (expiration < 0) {
		if (canOverflow) {
		    expiration = Long.MAX_VALUE;
	        } else {
	            expiration = 0;
	        }
	    }
	}
	return expiration;
    }
    
    private OurAbstractLease(long expiration, int serialFormat){
	this.expiration = expiration;
	this.serialFormat = serialFormat;
    }

    /** Return the lease expiration. */
    public long getExpiration() {
	return expiration;
    }

    /** Return the serialization format for the expiration. */
    public int getSerialFormat() {
	return serialFormat;
    }

    /** Set the serialization format for the expiration. */
    public void setSerialFormat(int format) {
	if (format != Lease.DURATION && format != Lease.ABSOLUTE)
	    throw new IllegalArgumentException("invalid serial format");
	serialFormat = format;
    }

    /** Renew the lease for a duration relative to now. */
    public void renew(long duration)
	throws UnknownLeaseException, LeaseDeniedException, RemoteException
    {
	expiration = doRenew(duration) + System.currentTimeMillis();
	// We added two positive numbers, so if the result is negative
	// we must have overflowed, truncate to Long.MAX_VALUE
	if (expiration < 0) 
	    expiration = Long.MAX_VALUE;
    }

    /**
     * Renew the lease for a duration relative to now, and return
     * the duration actually granted.
     */
    protected abstract long doRenew(long duration)
	throws UnknownLeaseException, LeaseDeniedException, RemoteException;

    /**
     * @serialData a long, which is the absolute expiration if serialFormat
     * is ABSOLUTE, or the relative duration if serialFormat is DURATION
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
	stream.defaultWriteObject();
	stream.writeLong(serialFormat == Lease.ABSOLUTE ?
			 expiration : expiration - System.currentTimeMillis());
    }

    /**
     * If serialFormat is DURATION, add the current time to the expiration,
     * to make it absolute (and if the result of the addition is negative,
     * correct the overflow by resetting the expiration to Long.MAX_VALUE),
     * or correct the underflow by resetting the expiration to zero.
     */
    private void readObject(ObjectInputStream stream)
	throws IOException, ClassNotFoundException
    {
	stream.defaultReadObject();
	expiration = stream.readLong();
	if (serialFormat == Lease.DURATION) {
	    boolean canOverflow = (expiration > 0);
	    expiration += System.currentTimeMillis();

	    // If we added two positive numbers, and the result is negative
	    // we must have overflowed, truncate to Long.MAX_VALUE. If we
	    // added a negative to a positive and the result is negative we
	    // must have underflowed, so limit to zero.
	    if (expiration < 0) {
		if (canOverflow) {
		    expiration = Long.MAX_VALUE;
	        } else {
	            expiration = 0;
	        }
	    }
	}
    }
    
    @ReadInput
    static ReadObject getRO(){
	return new RO();
    }
    
    private static class RO implements ReadObject {
	long expiration;

	@Override
	public void read(ObjectInput stream) throws IOException, ClassNotFoundException {
	    expiration = stream.readLong();
	}
	
    }
}
